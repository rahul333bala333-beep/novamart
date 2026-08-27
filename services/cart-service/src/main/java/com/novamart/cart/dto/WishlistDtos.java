package com.novamart.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WishlistDtos {

    private WishlistDtos() {
    }

    public record AddWishlistItemRequest(
            @NotNull UUID productId) {
    }

    public record WishlistItemResponse(
            UUID id,
            UUID productId,
            String name,
            String slug,
            String imageUrl,
            BigDecimal price,
            String currency,
            int availableQuantity,
            boolean inStock,
            Instant addedAt) {
    }

    public record WishlistResponse(
            UUID userId,
            List<WishlistItemResponse> items,
            int totalItems) {
    }
}
