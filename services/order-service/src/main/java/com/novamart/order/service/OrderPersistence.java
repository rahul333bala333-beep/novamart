package com.novamart.order.service;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.order.domain.Order;
import com.novamart.order.domain.OrderItem;
import com.novamart.order.domain.OrderStatus;
import com.novamart.order.domain.ShippingAddress;
import com.novamart.order.dto.OrderDtos.AddressSnapshot;
import com.novamart.order.dto.OrderDtos.CartLine;
import com.novamart.order.dto.OrderDtos.CreateOrderRequest;
import com.novamart.order.dto.OrderDtos.OrderResponse;
import com.novamart.order.dto.OrderDtos.ProductSnapshot;
import com.novamart.order.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The individually-committed database steps of the checkout saga.
 *
 * <p>Each method is its own transaction, and that is the entire point. An
 * earlier version wrapped the whole of {@code CheckoutOrchestrator.checkout} in
 * a single {@code @Transactional}, which looked tidy and was wrong in two ways:
 *
 * <ol>
 *   <li>It contradicted the design. A saga exists precisely because the steps
 *       <em>cannot</em> share a transaction; pretending they do in the one
 *       service that can hold a transaction is self-deception.</li>
 *   <li>It lost data. When payment was declined, the orchestrator marked the
 *       order CANCELLED and then threw, and the throw rolled back the very row
 *       it had just written. The declined order silently vanished, while the
 *       notification service kept a message referring to it. Revenue reporting
 *       and support both lost the record of a real customer attempt.</li>
 * </ol>
 *
 * <p>Splitting the writes means a declined checkout leaves a durable CANCELLED
 * order, exactly as a real one would.
 */
@Component
public class OrderPersistence {

    private static final int DELIVERY_DAYS = 5;

    private final OrderRepository orders;
    private final OrderNumbers orderNumbers;
    private final PricingPolicy pricing;
    private final CouponService couponService;

    public OrderPersistence(OrderRepository orders, OrderNumbers orderNumbers, PricingPolicy pricing, CouponService couponService) {
        this.orders = orders;
        this.orderNumbers = orderNumbers;
        this.pricing = pricing;
        this.couponService = couponService;
    }

    /**
     * Looks up a previous order by its idempotency key.
     *
     * <p>Returns a fully-mapped response rather than the entity. Handing an
     * entity back across the transaction boundary and mapping it afterwards
     * throws {@code LazyInitializationException} the moment the mapper touches
     * {@code items}, because the persistence context has already closed. Mapping
     * inside the transaction is the fix, and returning a DTO makes it impossible
     * to reintroduce the bug.
     */
    @Transactional(readOnly = true)
    public Optional<OrderResponse> findByIdempotencyKey(UUID userId, String key) {
        return orders.findByUserIdAndIdempotencyKey(userId, key).map(order -> {
            order.getItems().size();
            return OrderResponse.from(order);
        });
    }

    /** Step 4: write the order as PENDING and commit it before payment is attempted. */
    @Transactional
    public Order createPending(UUID userId, CreateOrderRequest request, AddressSnapshot address,
                               List<CartLine> lines, Map<UUID, ProductSnapshot> products,
                               String idempotencyKey) {

        Order order = Order.open(
                orderNumbers.next(),
                userId,
                new ShippingAddress(address.label(), address.recipientName(), address.phone(),
                        address.line1(), address.line2(), address.city(), address.state(),
                        address.postalCode(), address.country()),
                request.paymentMethod().name(),
                request.notes(),
                idempotencyKey);

        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = "INR";
        for (CartLine line : lines) {
            ProductSnapshot product = products.get(line.productId());
            OrderItem item = OrderItem.snapshot(order, product.id(), product.sku(), product.name(),
                    product.slug(), product.imageUrl(), product.price(), line.quantity());
            order.addItem(item);
            subtotal = subtotal.add(item.getLineTotal());
            currency = product.currency();
        }
        BigDecimal deliveryFee = pricing.deliveryFeeFor(subtotal);
        BigDecimal discount = couponService.calculateAndConsumeDiscount(request.couponCode(), subtotal, deliveryFee);
        order.applyTotals(subtotal, deliveryFee, discount, currency);

        return orders.save(order);
    }

    /**
     * Step 7a: payment settled, so the order becomes CONFIRMED.
     *
     * <p>Returns a mapped response for the same reason as
     * {@link #findByIdempotencyKey}: the caller runs outside any transaction, so
     * anything it maps later would touch a detached collection.
     */
    @Transactional
    public OrderResponse confirm(UUID orderId, UUID paymentId, String paymentStatus) {
        Order order = load(orderId);
        order.attachPayment(paymentId, paymentStatus);
        order.transitionTo(OrderStatus.CONFIRMED, "Payment " + paymentStatus.toLowerCase());
        order.setEstimatedDeliveryDate(
                LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC).plusDays(DELIVERY_DAYS));
        return OrderResponse.from(order);
    }

    /**
     * Step 7b: payment failed, so the order becomes CANCELLED and stays on record.
     *
     * <p>Committing the failure is the whole reason this class exists.
     */
    @Transactional
    public OrderResponse fail(UUID orderId, UUID paymentId, String reason) {
        Order order = load(orderId);
        order.attachPayment(paymentId, "FAILED");
        order.cancel(reason);
        return OrderResponse.from(order);
    }

    private Order load(UUID orderId) {
        return orders.findDetailById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
    }
}
