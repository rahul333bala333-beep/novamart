package com.novamart.payment.domain;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uq_payments_transaction_ref", columnNames = "transaction_reference"),
        indexes = {
                @Index(name = "idx_payments_order", columnList = "order_id"),
                @Index(name = "idx_payments_user", columnList = "user_id")
        })
public class Payment {

    public enum Method {
        /** Simulated card rail. Settles on verification. No card details are collected. */
        MOCK_CARD,
        /** Collected on handover, so it stays INITIATED until delivery. */
        CASH_ON_DELIVERY
    }

    public enum Status {
        INITIATED, SUCCESS, FAILED, REFUNDED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private Method method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    /** The reference a shopper quotes when asking about a payment. */
    @Column(name = "transaction_reference", nullable = false, length = 40)
    private String transactionReference;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<Transaction> transactions = new ArrayList<>();

    protected Payment() {
    }

    public static Payment initiate(UUID orderId, UUID userId, BigDecimal amount,
                                   String currency, Method method, String reference) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.orderId = orderId;
        payment.userId = userId;
        payment.amount = amount;
        payment.currency = currency;
        payment.method = method;
        payment.status = Status.INITIATED;
        payment.transactionReference = reference;
        payment.createdAt = Instant.now();
        payment.addTransaction(Transaction.Type.AUTHORIZE, amount, reference,
                "Payment opened against order " + orderId);
        return payment;
    }

    public void markSuccessful(String gatewayReference) {
        requireSettleable();
        this.status = Status.SUCCESS;
        this.settledAt = Instant.now();
        addTransaction(Transaction.Type.CAPTURE, amount, gatewayReference, "Payment captured");
    }

    public void markFailed(String gatewayReference, String reason) {
        requireSettleable();
        this.status = Status.FAILED;
        this.failureReason = reason;
        this.settledAt = Instant.now();
        addTransaction(Transaction.Type.DECLINE, amount, gatewayReference, reason);
    }

    /**
     * Refunds a captured payment. Only a successful payment can be refunded;
     * refunding a failed one would move money that was never taken.
     */
    public void markRefunded(String reason) {
        if (status != Status.SUCCESS) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_STATE,
                    "Only a captured payment can be refunded; this one is " + status);
        }
        this.status = Status.REFUNDED;
        addTransaction(Transaction.Type.REFUND, amount, transactionReference, reason);
    }

    private void requireSettleable() {
        if (status != Status.INITIATED) {
            throw new ApiException(ErrorCode.INVALID_PAYMENT_STATE,
                    "Payment is already " + status + " and cannot be settled again");
        }
    }

    private void addTransaction(Transaction.Type type, BigDecimal amount,
                                String gatewayReference, String message) {
        transactions.add(Transaction.record(this, type, amount, gatewayReference, message));
    }

    public boolean isSettled() {
        return status != Status.INITIATED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Method getMethod() {
        return method;
    }

    public Status getStatus() {
        return status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
