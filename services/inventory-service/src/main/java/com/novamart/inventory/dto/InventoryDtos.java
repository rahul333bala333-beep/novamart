package com.novamart.inventory.dto;

import com.novamart.inventory.domain.InventoryItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record StockMovementRequest(
            @NotNull @Min(1) Integer quantity,
            @Size(max = 80) String referenceId) {
    }

    public record UpdateInventoryRequest(
            @NotNull @Min(0) Integer totalQuantity,
            @Min(0) Integer reorderThreshold) {
    }

    public record InventoryResponse(
            UUID productId,
            int totalQuantity,
            int reservedQuantity,
            int availableQuantity,
            int reorderThreshold,
            boolean inStock,
            boolean lowStock,
            Instant updatedAt) {

        public static InventoryResponse from(InventoryItem item) {
            return new InventoryResponse(
                    item.getProductId(),
                    item.getTotalQuantity(),
                    item.getReservedQuantity(),
                    item.availableQuantity(),
                    item.getReorderThreshold(),
                    item.isInStock(),
                    item.isLowStock(),
                    item.getUpdatedAt());
        }
    }
}
