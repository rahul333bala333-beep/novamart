package com.novamart.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons", uniqueConstraints = @UniqueConstraint(name = "uq_coupons_code", columnNames = "code"))
public class Coupon {

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT,
        FREE_SHIPPING
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Coupon() {
    }

    public static Coupon create(String code, DiscountType type, BigDecimal value, BigDecimal minOrder, BigDecimal maxDiscount, Integer usageLimit, Instant expiresAt) {
        Coupon coupon = new Coupon();
        coupon.id = UUID.randomUUID();
        coupon.code = code.trim().toUpperCase();
        coupon.discountType = type;
        coupon.discountValue = value != null ? value : BigDecimal.ZERO;
        coupon.minOrderAmount = minOrder != null ? minOrder : BigDecimal.ZERO;
        coupon.maxDiscount = maxDiscount;
        coupon.usageLimit = usageLimit;
        coupon.usageCount = 0;
        coupon.active = true;
        coupon.expiresAt = expiresAt;
        coupon.createdAt = Instant.now();
        return coupon;
    }

    public boolean isValid(BigDecimal orderSubtotal) {
        if (!active) return false;
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) return false;
        if (usageLimit != null && usageCount >= usageLimit) return false;
        if (orderSubtotal != null && orderSubtotal.compareTo(minOrderAmount) < 0) return false;
        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal orderSubtotal, BigDecimal deliveryFee) {
        if (!isValid(orderSubtotal)) {
            return BigDecimal.ZERO;
        }
        BigDecimal calculated = switch (discountType) {
            case PERCENTAGE -> orderSubtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> discountValue;
            case FREE_SHIPPING -> deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        };

        if (maxDiscount != null && calculated.compareTo(maxDiscount) > 0) {
            calculated = maxDiscount;
        }
        if (calculated.compareTo(orderSubtotal) > 0) {
            calculated = orderSubtotal;
        }
        return calculated.setScale(2, RoundingMode.HALF_UP);
    }

    public void incrementUsage() {
        this.usageCount++;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
