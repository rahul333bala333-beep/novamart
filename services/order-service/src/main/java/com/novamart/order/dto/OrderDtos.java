package com.novamart.order.dto;

import com.novamart.order.domain.Order;
import com.novamart.order.domain.OrderEvent;
import com.novamart.order.domain.OrderItem;
import com.novamart.order.domain.OrderStatus;
import com.novamart.order.domain.ShippingAddress;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    // ---------- requests ----------

    public record CreateOrderRequest(
            @NotNull UUID addressId,
            @NotNull PaymentMethod paymentMethod,
            @Size(max = 500) String notes,
            @Size(max = 30) String couponCode,
            /*
             * Demo switch that forces the simulated gateway to decline, so the
             * compensation path can be shown deliberately rather than waited for.
             * A real deployment would drop this field; it is documented as
             * demo-only in the API contract rather than hidden.
             */
            boolean simulateFailure) {

        public CreateOrderRequest(UUID addressId, PaymentMethod paymentMethod, String notes, boolean simulateFailure) {
            this(addressId, paymentMethod, notes, null, simulateFailure);
        }
    }

    public enum PaymentMethod {
        MOCK_CARD, CASH_ON_DELIVERY
    }

    public record CancelOrderRequest(@Size(max = 500) String reason) {
    }

    public record UpdateStatusRequest(
            @NotNull OrderStatus status,
            @Size(max = 500) String note) {
    }

    // ---------- responses ----------

    public record AddressResponse(
            String label, String recipientName, String phone, String line1, String line2,
            String city, String state, String postalCode, String country) {

        static AddressResponse from(ShippingAddress a) {
            if (a == null) {
                return null;
            }
            return new AddressResponse(a.getLabel(), a.getRecipientName(), a.getPhone(),
                    a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                    a.getPostalCode(), a.getCountry());
        }
    }

    public record OrderItemResponse(
            UUID productId, String sku, String name, String slug, String imageUrl,
            BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {

        static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getProductId(), i.getSku(), i.getName(), i.getSlug(),
                    i.getImageUrl(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal());
        }
    }

    public record OrderEventResponse(OrderStatus status, String note, Instant occurredAt) {

        static OrderEventResponse from(OrderEvent e) {
            return new OrderEventResponse(e.getStatus(), e.getNote(), e.getOccurredAt());
        }
    }

    public record OrderResponse(
            UUID id,
            String orderNumber,
            UUID userId,
            OrderStatus status,
            List<OrderItemResponse> items,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal discount,
            BigDecimal total,
            String currency,
            AddressResponse shippingAddress,
            UUID paymentId,
            String paymentStatus,
            String paymentMethod,
            LocalDate estimatedDeliveryDate,
            String notes,
            String cancelledReason,
            Instant placedAt) {

        public static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(), o.getOrderNumber(), o.getUserId(), o.getStatus(),
                    o.getItems().stream().map(OrderItemResponse::from).toList(),
                    o.getSubtotal(), o.getDeliveryFee(), o.getDiscount(), o.getTotal(), o.getCurrency(),
                    AddressResponse.from(o.getShippingAddress()),
                    o.getPaymentId(), o.getPaymentStatus(), o.getPaymentMethod(),
                    o.getEstimatedDeliveryDate(), o.getNotes(), o.getCancelledReason(), o.getPlacedAt());
        }
    }

    public record OrderDetailResponse(OrderResponse order, List<OrderEventResponse> timeline) {

        public static OrderDetailResponse from(Order o) {
            return new OrderDetailResponse(
                    OrderResponse.from(o),
                    o.getTimeline().stream().map(OrderEventResponse::from).toList());
        }
    }

    // ---------- dashboard ----------

    public record OrderStatsResponse(
            BigDecimal totalRevenue,
            long totalOrders,
            long pendingOrders,
            long cancelledOrders,
            BigDecimal averageOrderValue,
            List<DailyRevenue> revenueByDay,
            List<StatusCount> statusBreakdown) {
    }

    public record DailyRevenue(LocalDate date, BigDecimal revenue, long orders) {
    }

    public record StatusCount(OrderStatus status, long count) {
    }

    // ---------- shapes read from other services ----------

    public record CartLine(UUID productId, int quantity) {
    }

    public record ProductSnapshot(UUID id, String sku, String name, String slug,
                                  String imageUrl, BigDecimal price, String currency) {
    }

    public record UserSnapshot(UUID id, String firstName, String email) {
    }

    public record AddressSnapshot(String label, String recipientName, String phone,
                                  String line1, String line2, String city, String state,
                                  String postalCode, String country) {
    }

    public record PaymentSnapshot(UUID id, String status, String transactionReference, String failureReason) {
    }
}
