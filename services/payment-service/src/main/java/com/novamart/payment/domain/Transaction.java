package com.novamart.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One step in a payment's life, appended and never modified.
 *
 * <p>A payment row only ever shows where things ended up. When a shopper
 * disputes a charge the question is what happened and when, which is what this
 * table answers.
 */
@Entity
@Table(name = "payment_transactions")
public class Transaction {

    public enum Type {
        AUTHORIZE, CAPTURE, DECLINE, REFUND
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_transactions_payment"))
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "gateway_reference", nullable = false, length = 60)
    private String gatewayReference;

    @Column(name = "message", length = 300)
    private String message;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected Transaction() {
    }

    static Transaction record(Payment payment, Type type, BigDecimal amount,
                              String gatewayReference, String message) {
        Transaction tx = new Transaction();
        tx.id = UUID.randomUUID();
        tx.payment = payment;
        tx.type = type;
        tx.amount = amount;
        tx.gatewayReference = gatewayReference;
        tx.message = message;
        tx.occurredAt = Instant.now();
        return tx;
    }

    public UUID getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public String getMessage() {
        return message;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
