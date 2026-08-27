package com.novamart.inventory.domain;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * The stock record for one product.
 *
 * <p>Two counters, not one. {@code totalQuantity} is what physically exists;
 * {@code reservedQuantity} is the part of it already spoken for by an order that
 * is mid-checkout. Availability is the difference, and it is derived rather than
 * stored so the three numbers can never disagree.
 *
 * <p>The three-step protocol is what keeps checkout honest:
 *
 * <ul>
 *   <li>{@link #reserve} moves units from available to reserved before payment</li>
 *   <li>{@link #commit} removes them for good once payment has settled</li>
 *   <li>{@link #release} puts them back when payment fails or an order is cancelled</li>
 * </ul>
 *
 * <p>Decrementing stock at payment time instead would oversell whenever two
 * shoppers reached the gateway together; decrementing at add-to-cart would let an
 * abandoned cart hold stock forever.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    /**
     * The product id doubles as the primary key. A product has exactly one stock
     * record, so a separate surrogate key would add a column and an index without
     * making anything expressible that is not already.
     */
    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold = 5;

    /**
     * Optimistic lock. The service also takes a pessimistic row lock for the
     * read-modify-write paths, but this version column is a second line of
     * defence that also protects the paths that do not lock.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public static InventoryItem create(UUID productId, int totalQuantity, int reorderThreshold) {
        InventoryItem item = new InventoryItem();
        item.productId = productId;
        item.totalQuantity = totalQuantity;
        item.reservedQuantity = 0;
        item.reorderThreshold = reorderThreshold;
        item.updatedAt = Instant.now();
        return item;
    }

    public int availableQuantity() {
        return totalQuantity - reservedQuantity;
    }

    public boolean isInStock() {
        return availableQuantity() > 0;
    }

    public boolean isLowStock() {
        return availableQuantity() <= reorderThreshold;
    }

    /** Holds units for an order that has not yet paid. */
    public void reserve(int quantity) {
        requirePositive(quantity);
        if (availableQuantity() < quantity) {
            throw new ApiException(ErrorCode.INSUFFICIENT_STOCK,
                    "Only " + availableQuantity() + " available, " + quantity + " requested");
        }
        reservedQuantity += quantity;
        touch();
    }

    /** Returns held units to the available pool. The compensation for {@link #reserve}. */
    public void release(int quantity) {
        requirePositive(quantity);
        if (reservedQuantity < quantity) {
            throw new ApiException(ErrorCode.INVALID_STOCK_OPERATION,
                    "Cannot release " + quantity + " units when only " + reservedQuantity + " are reserved");
        }
        reservedQuantity -= quantity;
        touch();
    }

    /** Consumes a reservation once payment has settled; the units leave the warehouse. */
    public void commit(int quantity) {
        requirePositive(quantity);
        if (reservedQuantity < quantity) {
            throw new ApiException(ErrorCode.INVALID_STOCK_OPERATION,
                    "Cannot commit " + quantity + " units when only " + reservedQuantity + " are reserved");
        }
        reservedQuantity -= quantity;
        totalQuantity -= quantity;
        touch();
    }

    /**
     * Sets the physical count during a stock take.
     *
     * <p>Refuses to go below what is already reserved: those units belong to
     * orders in flight, and allowing the total to drop under them would produce a
     * negative availability that every downstream check would then misread.
     */
    public void adjustTotal(int newTotal, int newThreshold) {
        if (newTotal < 0) {
            throw new ApiException(ErrorCode.INVALID_STOCK_OPERATION, "Stock cannot be negative");
        }
        if (newTotal < reservedQuantity) {
            throw new ApiException(ErrorCode.INVALID_STOCK_OPERATION,
                    "Cannot set stock to " + newTotal + " while " + reservedQuantity
                            + " units are reserved for orders in progress");
        }
        this.totalQuantity = newTotal;
        this.reorderThreshold = newThreshold;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new ApiException(ErrorCode.INVALID_STOCK_OPERATION, "Quantity must be greater than zero");
        }
    }

    public UUID getProductId() {
        return productId;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
