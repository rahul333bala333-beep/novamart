package com.novamart.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An append-only record of every stock movement.
 *
 * <p>Without this, the only thing anyone can see is the current number, and the
 * question that actually gets asked during an investigation is "how did it get
 * to that number". Rows are never updated or deleted.
 */
@Entity
@Table(name = "stock_transactions",
        indexes = {
                @Index(name = "idx_stock_transactions_product", columnList = "product_id"),
                @Index(name = "idx_stock_transactions_reference", columnList = "reference_id")
        })
public class StockTransaction {

    public enum Type {
        /** Units held for an order that has not yet paid. */
        RESERVE,
        /** Held units returned after a failed payment or a cancellation. */
        RELEASE,
        /** Held units consumed after payment settled. */
        COMMIT,
        /** An administrator corrected the physical count. */
        MANUAL_ADJUSTMENT,
        /** A stock record was created for a new product. */
        INITIAL
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private Type type;

    /** Signed against available stock: negative removes, positive returns. */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "resulting_total", nullable = false)
    private int resultingTotal;

    @Column(name = "resulting_reserved", nullable = false)
    private int resultingReserved;

    /** The order this movement belongs to, when there is one. */
    @Column(name = "reference_id", length = 80)
    private String referenceId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected StockTransaction() {
    }

    public static StockTransaction record(InventoryItem item, Type type, int quantity, String referenceId) {
        StockTransaction tx = new StockTransaction();
        tx.id = UUID.randomUUID();
        tx.productId = item.getProductId();
        tx.type = type;
        tx.quantity = quantity;
        tx.resultingTotal = item.getTotalQuantity();
        tx.resultingReserved = item.getReservedQuantity();
        tx.referenceId = referenceId;
        tx.occurredAt = Instant.now();
        return tx;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public Type getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
