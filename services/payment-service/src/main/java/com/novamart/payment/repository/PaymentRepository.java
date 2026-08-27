package com.novamart.payment.repository;

import com.novamart.payment.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = "transactions")
    Optional<Payment> findWithTransactionsById(UUID id);

    Optional<Payment> findByOrderId(UUID orderId);

    @Query("select p from Payment p where :status is null or p.status = :status")
    Page<Payment> findFiltered(@Param("status") Payment.Status status, Pageable pageable);
}
