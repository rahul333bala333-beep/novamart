package com.novamart.order.dto;

import com.novamart.order.domain.Coupon.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class CouponDtos {

    private CouponDtos() {
    }

    public record ValidateCouponRequest(
            @NotBlank String code,
            @NotNull @DecimalMin("0.0") BigDecimal orderSubtotal) {
    }

    public record ValidateCouponResponse(
            boolean valid,
            String code,
            DiscountType discountType,
            BigDecimal discountAmount,
            String message) {
    }

    public record CreateCouponRequest(
            @NotBlank @Size(max = 30) String code,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin("0.0") BigDecimal discountValue,
            @DecimalMin("0.0") BigDecimal minOrderAmount,
            BigDecimal maxDiscount,
            Integer usageLimit,
            Instant expiresAt) {
    }

    public record CouponResponse(
            UUID id,
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            BigDecimal maxDiscount,
            Integer usageLimit,
            int usageCount,
            boolean active,
            Instant expiresAt,
            Instant createdAt) {
    }
}
