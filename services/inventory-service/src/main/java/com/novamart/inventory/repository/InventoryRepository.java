package com.novamart.inventory.repository;

import com.novamart.inventory.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {

    /**
     * Loads a stock row under a write lock, issuing {@code SELECT ... FOR UPDATE}.
     *
     * <p>This is the heart of the service. Reserving stock is a read-modify-write:
     * check availability, then decrement. Without a lock, two concurrent checkouts
     * can both read "1 available", both decide they may proceed, and both write a
     * reservation, selling the same unit twice. The lock serialises them so the
     * second waits and then correctly sees zero.
     *
     * <p>A pessimistic lock is the right instrument here rather than a retry loop
     * on the optimistic version: contention on a popular product is expected
     * rather than exceptional, and a shopper would rather wait a few milliseconds
     * than be told to try again.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.productId = :productId")
    Optional<InventoryItem> findForUpdate(UUID productId);

    @Query("""
            select i from InventoryItem i
            where :lowStockOnly = false
               or (i.totalQuantity - i.reservedQuantity) <= i.reorderThreshold
            """)
    Page<InventoryItem> findFiltered(boolean lowStockOnly, Pageable pageable);

    @Query("select count(i) from InventoryItem i where (i.totalQuantity - i.reservedQuantity) <= i.reorderThreshold")
    long countLowStock();
}
