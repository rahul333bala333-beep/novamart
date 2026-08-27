package com.novamart.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record CreateReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 3000) String comment) {
    }

    public record UpdateReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 3000) String comment) {
    }

    public record ReviewResponse(
            UUID id,
            UUID productId,
            UUID userId,
            String userName,
            int rating,
            String title,
            String comment,
            boolean verifiedPurchase,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ProductReviewSummaryResponse(
            UUID productId,
            BigDecimal ratingAverage,
            int ratingCount,
            Map<Integer, Long> ratingDistribution) {
    }
}
