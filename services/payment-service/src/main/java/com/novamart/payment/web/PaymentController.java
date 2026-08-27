package com.novamart.payment.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.api.PageResponse;
import com.novamart.payment.domain.Payment;
import com.novamart.payment.dto.PaymentDtos.CreatePaymentRequest;
import com.novamart.payment.dto.PaymentDtos.PaymentDetailResponse;
import com.novamart.payment.dto.PaymentDtos.PaymentResponse;
import com.novamart.payment.dto.PaymentDtos.VerifyPaymentRequest;
import com.novamart.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Validated
@Tag(name = "Payments", description = "Simulated payment capture and verification")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Open a payment against an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> create(
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Payment opened", paymentService.create(request)));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Settle a payment through the simulated gateway")
    public ApiResponse<PaymentResponse> verify(@PathVariable UUID id,
                                               @RequestBody(required = false) VerifyPaymentRequest request) {
        boolean simulateFailure = request != null && request.simulateFailure();
        return ApiResponse.of("Payment verified", paymentService.verify(id, simulateFailure));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @Operation(summary = "Refund a captured payment")
    public ApiResponse<PaymentResponse> refund(@PathVariable UUID id,
                                               @RequestParam(required = false) String reason) {
        return ApiResponse.of("Payment refunded", paymentService.refund(id, reason));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment with its transaction trail")
    public ApiResponse<PaymentDetailResponse> get(@PathVariable UUID id) {
        return ApiResponse.of("Payment retrieved", paymentService.detail(id));
    }

    @GetMapping("/by-order/{orderId}")
    @Operation(summary = "Find the payment for an order")
    public ApiResponse<PaymentResponse> byOrder(@PathVariable UUID orderId) {
        return ApiResponse.of("Payment retrieved", paymentService.byOrder(orderId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List payments (admin)")
    public ApiResponse<PageResponse<PaymentResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Payment.Status status) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.of("Payments retrieved", paymentService.list(status, pageable));
    }
}
