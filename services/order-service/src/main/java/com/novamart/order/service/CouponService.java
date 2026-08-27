package com.novamart.order.service;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.order.domain.Coupon;
import com.novamart.order.dto.CouponDtos.CouponResponse;
import com.novamart.order.dto.CouponDtos.CreateCouponRequest;
import com.novamart.order.dto.CouponDtos.ValidateCouponResponse;
import com.novamart.order.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final PricingPolicy pricingPolicy;

    public CouponService(CouponRepository couponRepository, PricingPolicy pricingPolicy) {
        this.couponRepository = couponRepository;
        this.pricingPolicy = pricingPolicy;
    }

    @Transactional(readOnly = true)
    public ValidateCouponResponse validate(String code, BigDecimal orderSubtotal) {
        if (code == null || code.isBlank()) {
            return new ValidateCouponResponse(false, null, null, BigDecimal.ZERO, "Coupon code cannot be empty");
        }

        Optional<Coupon> opt = couponRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isEmpty()) {
            return new ValidateCouponResponse(false, code, null, BigDecimal.ZERO, "Coupon code is invalid");
        }

        Coupon coupon = opt.get();
        if (!coupon.isActive()) {
            return new ValidateCouponResponse(false, coupon.getCode(), coupon.getDiscountType(), BigDecimal.ZERO, "Coupon is no longer active");
        }

        if (!coupon.isValid(orderSubtotal)) {
            if (coupon.getExpiresAt() != null && java.time.Instant.now().isAfter(coupon.getExpiresAt())) {
                return new ValidateCouponResponse(false, coupon.getCode(), coupon.getDiscountType(), BigDecimal.ZERO, "Coupon has expired");
            }
            if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
                return new ValidateCouponResponse(false, coupon.getCode(), coupon.getDiscountType(), BigDecimal.ZERO, "Coupon usage limit reached");
            }
            if (orderSubtotal != null && orderSubtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
                return new ValidateCouponResponse(false, coupon.getCode(), coupon.getDiscountType(), BigDecimal.ZERO,
                        "Minimum order amount for this coupon is ₹" + coupon.getMinOrderAmount());
            }
            return new ValidateCouponResponse(false, coupon.getCode(), coupon.getDiscountType(), BigDecimal.ZERO, "Coupon is not valid for this order");
        }

        BigDecimal deliveryFee = pricingPolicy.deliveryFeeFor(orderSubtotal != null ? orderSubtotal : BigDecimal.ZERO);
        BigDecimal discount = coupon.calculateDiscount(orderSubtotal, deliveryFee);

        return new ValidateCouponResponse(true, coupon.getCode(), coupon.getDiscountType(), discount, "Coupon applied successfully!");
    }

    @Transactional
    public BigDecimal calculateAndConsumeDiscount(String code, BigDecimal orderSubtotal, BigDecimal deliveryFee) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        Optional<Coupon> opt = couponRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Coupon coupon = opt.get();
        if (coupon.isValid(orderSubtotal)) {
            BigDecimal discount = coupon.calculateDiscount(orderSubtotal, deliveryFee);
            coupon.incrementUsage();
            couponRepository.save(coupon);
            log.info("Applied coupon {} giving discount {} on order subtotal {}", coupon.getCode(), discount, orderSubtotal);
            return discount;
        }
        return BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listAll() {
        return couponRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new ApiException(ErrorCode.CONFLICT, "Coupon with code " + request.code() + " already exists");
        }

        Coupon coupon = Coupon.create(
                request.code(),
                request.discountType(),
                request.discountValue(),
                request.minOrderAmount(),
                request.maxDiscount(),
                request.usageLimit(),
                request.expiresAt());

        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(UUID id, CreateCouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Coupon not found"));

        Coupon updated = Coupon.create(
                request.code(),
                request.discountType(),
                request.discountValue(),
                request.minOrderAmount(),
                request.maxDiscount(),
                request.usageLimit(),
                request.expiresAt());
        // Preserve id and usage count
        couponRepository.delete(coupon);
        return toResponse(couponRepository.save(updated));
    }

    @Transactional
    public void delete(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Coupon not found"));
        couponRepository.delete(coupon);
    }

    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(
                c.getId(),
                c.getCode(),
                c.getDiscountType(),
                c.getDiscountValue(),
                c.getMinOrderAmount(),
                c.getMaxDiscount(),
                c.getUsageLimit(),
                c.getUsageCount(),
                c.isActive(),
                c.getExpiresAt(),
                c.getCreatedAt());
    }
}
