package com.novamart.order.repository;

import com.novamart.order.domain.OrderNumberCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderNumberRepository extends JpaRepository<OrderNumberCounter, Integer> {

    /**
     * Takes the counter row under a write lock.
     *
     * <p>The lock is what makes the allocation safe: read-then-increment without
     * it hands the same number to two concurrent checkouts, and the unique
     * constraint on order_number then fails one of them at commit time with an
     * error that looks nothing like its cause.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OrderNumberCounter c where c.id = 1")
    Optional<OrderNumberCounter> lockCounter();
}
