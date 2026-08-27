package com.novamart.order.service;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.order.client.CheckoutGateways;
import com.novamart.order.domain.Order;
import com.novamart.order.dto.OrderDtos.AddressSnapshot;
import com.novamart.order.dto.OrderDtos.CartLine;
import com.novamart.order.dto.OrderDtos.CreateOrderRequest;
import com.novamart.order.dto.OrderDtos.OrderResponse;
import com.novamart.order.dto.OrderDtos.PaymentSnapshot;
import com.novamart.order.dto.OrderDtos.ProductSnapshot;
import com.novamart.order.dto.OrderDtos.UserSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a cart into a paid order across five services.
 *
 * <h2>Why this is a saga and not a transaction</h2>
 *
 * <p>Checkout changes state in four databases owned by four services: it
 * reserves stock in {@code inventory_db}, records an order in {@code order_db},
 * takes payment in {@code payment_db}, and empties {@code cart_db}. No database
 * transaction spans them, and a two-phase commit across HTTP would couple their
 * availability so tightly that separating them would buy nothing.
 *
 * <p>So consistency is reached the other way. Each step commits independently,
 * and every step that can fail has a compensating action that undoes it. The
 * system is eventually consistent rather than atomically consistent, and the
 * window is milliseconds.
 *
 * <p><b>This class is deliberately not {@code @Transactional}.</b> Wrapping the
 * orchestration in one transaction is the intuitive thing to reach for and it is
 * a mistake: a rollback at the payment step would erase the CANCELLED order that
 * records the customer's failed attempt. Database writes happen in
 * {@link OrderPersistence}, one committed step at a time.
 *
 * <h2>The sequence</h2>
 *
 * <pre>
 *   1. read the cart                      (cart-service)
 *   2. snapshot names and prices          (product-service)
 *   3. reserve stock, line by line        (inventory-service)   &lt;-- compensatable
 *   4. commit the order as PENDING        (order_db)
 *   5. open a payment                     (payment-service)
 *   6. settle the payment                 (payment-service)
 *   7a. success: commit reservations, empty the cart, CONFIRMED
 *   7b. failure: release reservations, CANCELLED, return 402
 * </pre>
 *
 * <h2>Ordering</h2>
 *
 * <p>Stock is reserved <em>before</em> payment. Taking money for an item that
 * turns out to be unavailable is far worse than briefly holding stock for an
 * order that does not complete: the first needs a refund and an apology, the
 * second resolves itself milliseconds later when the reservation is released.
 *
 * <p>Reservations are committed only <em>after</em> payment settles, which is why
 * inventory-service exposes reserve, release and commit rather than a single
 * decrement.
 */
@Service
public class CheckoutOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CheckoutOrchestrator.class);
    private static final int DELIVERY_DAYS = 5;

    private final OrderPersistence persistence;
    private final CheckoutGateways gateways;

    public CheckoutOrchestrator(OrderPersistence persistence, CheckoutGateways gateways) {
        this.persistence = persistence;
        this.gateways = gateways;
    }

    public OrderResponse checkout(UUID userId, CreateOrderRequest request, String idempotencyKey) {

        // ---- 0. idempotency ---------------------------------------------------
        // Checked before any side effect, so a replay costs nothing and cannot
        // charge a second time.
        if (StringUtils.hasText(idempotencyKey)) {
            Optional<OrderResponse> existing = persistence.findByIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent replay of key {} returning order {}",
                        idempotencyKey, existing.get().id());
                return existing.get();
            }
        }

        // ---- 1. read the cart -------------------------------------------------
        List<CartLine> lines = gateways.readCart(userId);
        if (lines.isEmpty()) {
            throw new ApiException(ErrorCode.CART_EMPTY);
        }

        // ---- 2. snapshot the catalogue ---------------------------------------
        List<UUID> productIds = lines.stream().map(CartLine::productId).toList();
        Map<UUID, ProductSnapshot> products = gateways.productsByIds(productIds);
        for (CartLine line : lines) {
            if (!products.containsKey(line.productId())) {
                throw new ApiException(ErrorCode.PRODUCT_NOT_FOUND,
                        "An item in your cart is no longer available. Please review your cart.");
            }
        }

        AddressSnapshot address = gateways.readAddress(userId, request.addressId());

        // ---- 3. reserve stock -------------------------------------------------
        // Reservations already taken are tracked so a failure on line five can
        // undo lines one to four. This list is the compensation log.
        List<Reservation> reserved = new ArrayList<>();
        String reference = "checkout:" + userId;
        try {
            for (CartLine line : lines) {
                gateways.reserve(line.productId(), line.quantity(), reference);
                reserved.add(new Reservation(line.productId(), line.quantity()));
            }
        } catch (RuntimeException ex) {
            releaseAll(reserved, reference);
            throw ex;   // carries INSUFFICIENT_STOCK through as a 409
        }

        // ---- 4. commit the order as PENDING ----------------------------------
        Order pending;
        try {
            pending = persistence.createPending(userId, request, address, lines, products, idempotencyKey);
        } catch (RuntimeException ex) {
            releaseAll(reserved, reference);
            throw ex;
        }

        UUID orderId = pending.getId();
        String orderRef = pending.getOrderNumber();

        // ---- 5 & 6. take payment ---------------------------------------------
        PaymentSnapshot payment;
        try {
            payment = gateways.createPayment(orderId, userId, pending.getTotal(),
                    pending.getCurrency(), request.paymentMethod().name());
            payment = gateways.verifyPayment(payment.id(), request.simulateFailure());
        } catch (RuntimeException ex) {
            log.warn("Payment step failed for order {}: {}", orderRef, ex.getMessage());
            releaseAll(reserved, orderRef);
            OrderResponse cancelled = persistence.fail(orderId, null, "Payment could not be processed");
            notifyPaymentFailed(userId, cancelled);
            throw new ApiException(ErrorCode.PAYMENT_FAILED,
                    "We could not process your payment. Nothing has been charged and your order was not placed.");
        }

        // ---- 7b. declined -----------------------------------------------------
        if ("FAILED".equals(payment.status())) {
            log.info("Payment declined for order {}; releasing {} reservation(s)", orderRef, reserved.size());
            releaseAll(reserved, orderRef);
            String reason = payment.failureReason() == null ? "Payment was declined" : payment.failureReason();
            OrderResponse cancelled = persistence.fail(orderId, payment.id(), reason);
            notifyPaymentFailed(userId, cancelled);
            throw new ApiException(ErrorCode.PAYMENT_FAILED, reason + ". Your order was not placed.");
        }

        // ---- 7a. settled ------------------------------------------------------
        // SUCCESS for a card, INITIATED for cash on delivery. Both confirm the
        // order: cash on delivery is captured at handover, which is why its
        // payment legitimately stays open.
        for (Reservation r : reserved) {
            gateways.commit(r.productId(), r.quantity(), orderRef);
        }
        OrderResponse confirmed = persistence.confirm(orderId, payment.id(), payment.status());

        gateways.clearCart(userId);
        notifyConfirmed(userId, confirmed, payment);

        log.info("Order {} confirmed for user {} ({} lines, total {})",
                orderRef, userId, confirmed.items().size(), confirmed.total());
        return confirmed;
    }

    // ------------------------------------------------------------------ helpers

    private void releaseAll(List<Reservation> reserved, String reference) {
        for (Reservation r : reserved) {
            gateways.releaseQuietly(r.productId(), r.quantity(), reference);
        }
    }

    private void notifyConfirmed(UUID userId, OrderResponse order, PaymentSnapshot payment) {
        UserSnapshot user = safeUser(userId);
        gateways.notifyQuietly(userId, "ORDER_CONFIRMATION", user == null ? null : user.email(),
                "Your Nova Mart order " + order.orderNumber() + " is confirmed",
                "Thanks" + (user == null ? "" : ", " + user.firstName())
                        + ". We have received your order " + order.orderNumber()
                        + " for " + order.currency() + " " + order.total()
                        + ". Payment reference " + payment.transactionReference()
                        + ". Estimated delivery in " + DELIVERY_DAYS + " days.",
                order.id().toString());
    }

    private void notifyPaymentFailed(UUID userId, OrderResponse order) {
        UserSnapshot user = safeUser(userId);
        gateways.notifyQuietly(userId, "PAYMENT_FAILED", user == null ? null : user.email(),
                "We could not process your payment",
                "Your order " + order.orderNumber() + " could not be completed because the payment "
                        + "was not successful. Nothing has been charged, and any items held for you "
                        + "have been returned to stock.",
                order.id().toString());
    }

    /** The shopper's email is nice to have for a notification, never worth failing checkout over. */
    private UserSnapshot safeUser(UUID userId) {
        try {
            return gateways.readUser(userId);
        } catch (RuntimeException ex) {
            log.warn("Could not read user {} for notification: {}", userId, ex.getMessage());
            return null;
        }
    }

    private record Reservation(UUID productId, int quantity) {
    }
}
