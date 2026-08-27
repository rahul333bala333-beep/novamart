package com.novamart.order.domain;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_orders_number", columnNames = "order_number"),
                @UniqueConstraint(name = "uq_orders_idempotency", columnNames = {"user_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_orders_user_placed", columnList = "user_id, placed_at"),
                @Index(name = "idx_orders_status", columnList = "status")
        })
public class Order {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** What the shopper quotes in an email. Never the UUID. */
    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * The delivery address as it was at the moment of purchase.
     *
     * <p>Copied rather than referenced. If it held only an address id, a shopper
     * later correcting a typo in their address book would silently rewrite where
     * a parcel delivered six months ago was sent, and the order record would stop
     * describing what actually happened.
     */
    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;

    /**
     * Client-supplied key that makes checkout safe to retry.
     *
     * <p>Together with the unique constraint on (user_id, idempotency_key), a
     * double-clicked pay button or a retry after a dropped response returns the
     * original order instead of charging twice.
     */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<OrderEvent> timeline = new ArrayList<>();

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public static Order open(String orderNumber, UUID userId, ShippingAddress address,
                             String paymentMethod, String notes, String idempotencyKey) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.orderNumber = orderNumber;
        order.userId = userId;
        order.status = OrderStatus.PENDING;
        order.shippingAddress = address;
        order.paymentMethod = paymentMethod;
        order.paymentStatus = "INITIATED";
        order.notes = notes;
        order.idempotencyKey = idempotencyKey;
        order.currency = "INR";
        order.subtotal = BigDecimal.ZERO;
        order.deliveryFee = BigDecimal.ZERO;
        order.discount = BigDecimal.ZERO;
        order.total = BigDecimal.ZERO;
        order.placedAt = Instant.now();
        order.updatedAt = order.placedAt;
        order.timeline.add(OrderEvent.record(order, OrderStatus.PENDING, "Order placed"));
        return order;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void applyTotals(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal discount, String currency) {
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.discount = discount;
        this.total = subtotal.add(deliveryFee).subtract(discount);
        this.currency = currency;
        this.updatedAt = Instant.now();
    }

    public void attachPayment(UUID paymentId, String paymentStatus) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Moves the order forward, refusing anything the state machine disallows.
     * This is the single gate every status change passes through.
     */
    public void transitionTo(OrderStatus target, String note) {
        if (!status.canTransitionTo(target)) {
            throw new ApiException(ErrorCode.INVALID_ORDER_TRANSITION,
                    "An order that is " + status + " cannot become " + target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
        this.timeline.add(OrderEvent.record(this, target, note));
    }

    public void cancel(String reason) {
        if (!status.isCancellable()) {
            throw new ApiException(ErrorCode.INVALID_ORDER_TRANSITION,
                    "An order that is " + status + " can no longer be cancelled");
        }
        this.cancelledReason = reason;
        transitionTo(OrderStatus.CANCELLED, reason);
    }

    public void setEstimatedDeliveryDate(LocalDate date) {
        this.estimatedDeliveryDate = date;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public List<OrderEvent> getTimeline() {
        return timeline;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }
}
