package com.novamart.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) @Max(20) Integer quantity) {
    }

    public record UpdateCartItemRequest(
            @NotNull @Min(0) @Max(20) Integer quantity) {
    }

    /**
     * The cart as the shopper sees it, with names, prices and stock merged in
     * from the services that own them.
     */
    public record CartResponse(
            UUID id,
            UUID userId,
            List<CartItemResponse> items,
            BigDecimal subtotal,
            int totalQuantity,
            String currency,
            Instant updatedAt) {
    }

    public record CartItemResponse(
            UUID productId,
            String name,
            String slug,
            String imageUrl,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal,
            int availableQuantity,
            boolean inStock) {
    }

    /** A product snapshot as returned by product-service. */
    public record ProductSnapshot(
            UUID id,
            String name,
            String slug,
            String imageUrl,
            BigDecimal price,
            String currency) {
    }

    public record StockSnapshot(UUID productId, int availableQuantity, boolean inStock) {
    }
}
