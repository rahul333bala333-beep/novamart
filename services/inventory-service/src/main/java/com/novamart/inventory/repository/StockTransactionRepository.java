package com.novamart.inventory.repository;

import com.novamart.inventory.domain.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {

    List<StockTransaction> findTop20ByProductIdOrderByOccurredAtDesc(UUID productId);
}
