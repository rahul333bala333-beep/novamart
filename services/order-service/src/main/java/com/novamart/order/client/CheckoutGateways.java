package com.novamart.order.client;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.client.ServiceClientFactory;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.order.dto.OrderDtos.AddressSnapshot;
import com.novamart.order.dto.OrderDtos.CartLine;
import com.novamart.order.dto.OrderDtos.PaymentSnapshot;
import com.novamart.order.dto.OrderDtos.ProductSnapshot;
import com.novamart.order.dto.OrderDtos.UserSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every outbound call checkout makes, in one place.
 *
 * <p>Collecting them here keeps {@code CheckoutOrchestrator} readable as a
 * sequence of business steps rather than a wall of HTTP plumbing, and it means
 * there is exactly one place to look when asking what order-service depends on.
 *
 * <p>Errors are not swallowed. {@code ServiceClientFactory} rethrows a
 * downstream failure with its original error code, so an
 * {@code INSUFFICIENT_STOCK} raised by inventory-service still reaches the
 * shopper as a 409 with that code rather than being flattened into a 500.
 */
@Component
public class CheckoutGateways {

    private static final Logger log = LoggerFactory.getLogger(CheckoutGateways.class);

    private final RestClient cart;
    private final RestClient product;
    private final RestClient inventory;
    private final RestClient payment;
    private final RestClient notification;
    private final RestClient auth;

    public CheckoutGateways(ServiceClientFactory factory,
                            @Value("${novamart.services.cart-url}") String cartUrl,
                            @Value("${novamart.services.product-url}") String productUrl,
                            @Value("${novamart.services.inventory-url}") String inventoryUrl,
                            @Value("${novamart.services.payment-url}") String paymentUrl,
                            @Value("${novamart.services.notification-url}") String notificationUrl,
                            @Value("${novamart.services.auth-url}") String authUrl) {
        this.cart = factory.create(cartUrl, "cart");
        this.product = factory.create(productUrl, "product");
        this.inventory = factory.create(inventoryUrl, "inventory");
        this.payment = factory.create(paymentUrl, "payment");
        this.notification = factory.create(notificationUrl, "notification");
        this.auth = factory.create(authUrl, "auth");
    }

    // ---------- cart ----------

    public List<CartLine> readCart(UUID userId) {
        ApiResponse<Map<String, Object>> response = cart.get()
                .uri("/api/v1/internal/carts/{userId}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            return List.of();
        }
        Object rawItems = response.data().get("items");
        if (!(rawItems instanceof List<?> items)) {
            return List.of();
        }
        return items.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(m -> new CartLine(
                        UUID.fromString(String.valueOf(m.get("productId"))),
                        ((Number) m.get("quantity")).intValue()))
                .toList();
    }

    public void clearCart(UUID userId) {
        // Best effort. The order is already paid and confirmed at this point, so
        // a failure to empty the cart must not fail the checkout; the shopper
        // would rather have their order than a tidy cart.
        try {
            cart.delete().uri("/api/v1/internal/carts/{userId}", userId).retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Could not clear cart for user {} after checkout: {}", userId, ex.getMessage());
        }
    }

    // ---------- catalogue ----------

    public Map<UUID, ProductSnapshot> productsByIds(List<UUID> ids) {
        ApiResponse<List<Map<String, Object>>> response = product.post()
                .uri("/api/v1/products/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("productIds", ids))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        Map<UUID, ProductSnapshot> out = new HashMap<>();
        if (response == null || response.data() == null) {
            return out;
        }
        for (Map<String, Object> raw : response.data()) {
            UUID id = UUID.fromString(String.valueOf(raw.get("id")));
            out.put(id, new ProductSnapshot(id,
                    (String) raw.get("sku"),
                    (String) raw.get("name"),
                    (String) raw.get("slug"),
                    (String) raw.get("imageUrl"),
                    new BigDecimal(String.valueOf(raw.get("price"))),
                    (String) raw.getOrDefault("currency", "INR")));
        }
        return out;
    }

    // ---------- inventory ----------

    public void reserve(UUID productId, int quantity, String orderReference) {
        inventory.post()
                .uri("/api/v1/inventory/{productId}/reserve", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", quantity, "referenceId", orderReference))
                .retrieve()
                .toBodilessEntity();
    }

    public void commit(UUID productId, int quantity, String orderReference) {
        inventory.post()
                .uri("/api/v1/inventory/{productId}/commit", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", quantity, "referenceId", orderReference))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Returns units to stock when a confirmed order is cancelled.
     *
     * <p>Distinct from {@link #releaseQuietly}: by the time an order is
     * cancelled its reservation has already been committed, so the units are
     * gone from the total rather than merely held. Calling release here would
     * fail, because there is no reservation left to release. This adds the
     * quantity back to the on-hand count instead.
     */
    public void restockQuietly(UUID productId, int quantity, String orderReference) {
        try {
            ApiResponse<Map<String, Object>> current = inventory.get()
                    .uri("/api/v1/inventory/{productId}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            int total = current == null || current.data() == null ? 0
                    : ((Number) current.data().getOrDefault("totalQuantity", 0)).intValue();
            inventory.put()
                    .uri("/api/v1/inventory/{productId}", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("totalQuantity", total + quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.error("COMPENSATION FAILED: could not restock {} units of product {} after cancelling {}. "
                            + "Stock is understated and needs manual correction.",
                    quantity, productId, orderReference, ex);
        }
    }

    /**
     * Compensating action for {@link #reserve}.
     *
     * <p>Never allowed to throw. It runs on the failure path, where an exception
     * would mask the original error and leave the caller looking at the wrong
     * problem. A release that fails is logged loudly: the stock is stranded until
     * an administrator corrects it, and that is worth an alert.
     */
    public void releaseQuietly(UUID productId, int quantity, String orderReference) {
        try {
            inventory.post()
                    .uri("/api/v1/inventory/{productId}/release", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantity", quantity, "referenceId", orderReference))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.error("COMPENSATION FAILED: could not release {} units of product {} for {}. "
                            + "This stock is stranded and needs manual correction.",
                    quantity, productId, orderReference, ex);
        }
    }

    // ---------- payment ----------

    public PaymentSnapshot createPayment(UUID orderId, UUID userId, BigDecimal amount,
                                         String currency, String method) {
        ApiResponse<Map<String, Object>> response = payment.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("orderId", orderId, "userId", userId, "amount", amount,
                        "currency", currency, "method", method))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return toPayment(response);
    }

    public PaymentSnapshot verifyPayment(UUID paymentId, boolean simulateFailure) {
        ApiResponse<Map<String, Object>> response = payment.post()
                .uri("/api/v1/payments/{id}/verify", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("simulateFailure", simulateFailure))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return toPayment(response);
    }

    public void refundQuietly(UUID paymentId, String reason) {
        try {
            payment.post()
                    .uri("/api/v1/payments/{id}/refund?reason={reason}", paymentId, reason)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.error("COMPENSATION FAILED: could not refund payment {}. Manual refund required.",
                    paymentId, ex);
        }
    }

    // ---------- identity ----------

    public UserSnapshot readUser(UUID userId) {
        ApiResponse<Map<String, Object>> response = auth.get()
                .uri("/api/v1/internal/users/{userId}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        Map<String, Object> d = response.data();
        return new UserSnapshot(UUID.fromString(String.valueOf(d.get("id"))),
                (String) d.get("firstName"), (String) d.get("email"));
    }

    public AddressSnapshot readAddress(UUID userId, UUID addressId) {
        ApiResponse<Map<String, Object>> response = auth.get()
                .uri("/api/v1/internal/users/{userId}/addresses/{addressId}", userId, addressId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            throw new ApiException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        Map<String, Object> d = response.data();
        return new AddressSnapshot(
                (String) d.get("label"), (String) d.get("recipientName"), (String) d.get("phone"),
                (String) d.get("line1"), (String) d.get("line2"), (String) d.get("city"),
                (String) d.get("state"), (String) d.get("postalCode"), (String) d.get("country"));
    }

    // ---------- notification ----------

    public void notifyQuietly(UUID userId, String type, String recipient,
                              String subject, String body, String referenceId) {
        // A notification is a consequence of the order, not a precondition for it.
        // Failing checkout because an email could not be recorded would be absurd.
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("type", type);
            payload.put("channel", "EMAIL");
            payload.put("recipient", recipient);
            payload.put("subject", subject);
            payload.put("body", body);
            payload.put("referenceId", referenceId);
            notification.post()
                    .uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Notification {} for user {} could not be recorded: {}", type, userId, ex.getMessage());
        }
    }

    private static PaymentSnapshot toPayment(ApiResponse<Map<String, Object>> response) {
        if (response == null || response.data() == null) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "The payment service returned no payment");
        }
        Map<String, Object> d = response.data();
        return new PaymentSnapshot(
                UUID.fromString(String.valueOf(d.get("id"))),
                (String) d.get("status"),
                (String) d.get("transactionReference"),
                (String) d.get("failureReason"));
    }
}
