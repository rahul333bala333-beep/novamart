package com.novamart.inventory.service;

import com.novamart.common.api.PageResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.inventory.domain.InventoryItem;
import com.novamart.inventory.domain.StockTransaction;
import com.novamart.inventory.dto.InventoryDtos.InventoryResponse;
import com.novamart.inventory.repository.InventoryRepository;
import com.novamart.inventory.repository.StockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * All stock mutations, each inside a transaction that holds a row lock.
 *
 * <p>Every write follows the same shape: take the lock, mutate the aggregate,
 * append a transaction row. Keeping the ledger write in the same transaction as
 * the mutation means the two can never diverge, even if the request fails
 * afterwards.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventory;
    private final StockTransactionRepository transactions;

    public InventoryService(InventoryRepository inventory, StockTransactionRepository transactions) {
        this.inventory = inventory;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(UUID productId) {
        return InventoryResponse.from(load(productId));
    }

    /**
     * Resolves many stock records in one query.
     *
     * <p>Ids with no stock record are simply absent from the result rather than
     * raising an error, because "this product has never been stocked" is a normal
     * answer to a bulk question, not a failure of the request.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> byIds(List<UUID> productIds) {
        return inventory.findAllById(productIds).stream().map(InventoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> list(boolean lowStockOnly, Pageable pageable) {
        return PageResponse.from(inventory.findFiltered(lowStockOnly, pageable), InventoryResponse::from);
    }

    @Transactional(readOnly = true)
    public long lowStockCount() {
        return inventory.countLowStock();
    }

    @Transactional
    public InventoryResponse reserve(UUID productId, int quantity, String referenceId) {
        InventoryItem item = lock(productId);
        item.reserve(quantity);
        transactions.save(StockTransaction.record(item, StockTransaction.Type.RESERVE, -quantity, referenceId));
        log.info("Reserved {} of product {} for {}", quantity, productId, referenceId);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse release(UUID productId, int quantity, String referenceId) {
        InventoryItem item = lock(productId);
        item.release(quantity);
        transactions.save(StockTransaction.record(item, StockTransaction.Type.RELEASE, quantity, referenceId));
        log.info("Released {} of product {} for {}", quantity, productId, referenceId);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse commit(UUID productId, int quantity, String referenceId) {
        InventoryItem item = lock(productId);
        item.commit(quantity);
        transactions.save(StockTransaction.record(item, StockTransaction.Type.COMMIT, 0, referenceId));
        log.info("Committed {} of product {} for {}", quantity, productId, referenceId);
        return InventoryResponse.from(item);
    }

    /**
     * Sets the on-hand count, creating the record if the product has never had one.
     *
     * <p>Upserting rather than failing lets product-service seed stock for a newly
     * created product with the same call an administrator uses to correct a count
     * later, so there is one code path instead of two.
     */
    @Transactional
    public InventoryResponse upsert(UUID productId, int totalQuantity, Integer reorderThreshold) {
        InventoryItem item = inventory.findForUpdate(productId).orElse(null);
        if (item == null) {
            item = InventoryItem.create(productId, totalQuantity,
                    reorderThreshold == null ? 5 : reorderThreshold);
            inventory.save(item);
            transactions.save(StockTransaction.record(item, StockTransaction.Type.INITIAL, totalQuantity, null));
            log.info("Created stock record for product {} at {} units", productId, totalQuantity);
        } else {
            int delta = totalQuantity - item.getTotalQuantity();
            item.adjustTotal(totalQuantity,
                    reorderThreshold == null ? item.getReorderThreshold() : reorderThreshold);
            transactions.save(StockTransaction.record(item, StockTransaction.Type.MANUAL_ADJUSTMENT, delta, null));
            log.info("Adjusted stock for product {} by {} to {}", productId, delta, totalQuantity);
        }
        return InventoryResponse.from(item);
    }

    private InventoryItem load(UUID productId) {
        return inventory.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private InventoryItem lock(UUID productId) {
        return inventory.findForUpdate(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVENTORY_NOT_FOUND));
    }
}
