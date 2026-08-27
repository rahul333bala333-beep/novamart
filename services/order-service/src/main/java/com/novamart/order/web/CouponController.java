package com.novamart.order.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.order.dto.CouponDtos.CouponResponse;
import com.novamart.order.dto.CouponDtos.CreateCouponRequest;
import com.novamart.order.dto.CouponDtos.ValidateCouponRequest;
import com.novamart.order.dto.CouponDtos.ValidateCouponResponse;
import com.novamart.order.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Coupon validation and administration")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon code against order subtotal")
    public ApiResponse<ValidateCouponResponse> validate(@Valid @RequestBody ValidateCouponRequest request) {
        return ApiResponse.of("Coupon validation result", couponService.validate(request.code(), request.orderSubtotal()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all coupons (admin)")
    public ApiResponse<List<CouponResponse>> list() {
        return ApiResponse.of("Coupons retrieved", couponService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new coupon (admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Coupon created", couponService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a coupon (admin)")
    public ApiResponse<CouponResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateCouponRequest request) {
        return ApiResponse.of("Coupon updated", couponService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a coupon (admin)")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID id) {
        couponService.delete(id);
        return ResponseEntity.ok(ApiResponse.of("Coupon deleted", "SUCCESS"));
    }
}
