package com.novamart.order.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.api.PageResponse;
import com.novamart.common.security.CurrentUser;
import com.novamart.order.domain.OrderStatus;
import com.novamart.order.dto.OrderDtos.CancelOrderRequest;
import com.novamart.order.dto.OrderDtos.CreateOrderRequest;
import com.novamart.order.dto.OrderDtos.OrderDetailResponse;
import com.novamart.order.dto.OrderDtos.OrderResponse;
import com.novamart.order.dto.OrderDtos.OrderStatsResponse;
import com.novamart.order.dto.OrderDtos.UpdateStatusRequest;
import com.novamart.order.service.CheckoutOrchestrator;
import com.novamart.order.service.OrderService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Validated
@Tag(name = "Orders", description = "Order placement, history and lifecycle")
public class OrderController {

    private final CheckoutOrchestrator checkout;
    private final OrderService orderService;

    public OrderController(CheckoutOrchestrator checkout, OrderService orderService) {
        this.checkout = checkout;
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place an order (checkout)")
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        OrderResponse order = checkout.checkout(CurrentUser.requireId(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Order placed successfully", order));
    }

    @GetMapping
    @Operation(summary = "List orders; a shopper sees only their own")
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID userId) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        return ApiResponse.of("Orders retrieved", orderService.list(status, userId, pageable));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aggregated order metrics (admin)")
    public ApiResponse<OrderStatsResponse> stats() {
        return ApiResponse.of("Metrics retrieved", orderService.stats());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one order with its status timeline")
    public ApiResponse<OrderDetailResponse> get(@PathVariable UUID id) {
        return ApiResponse.of("Order retrieved", orderService.detail(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ApiResponse<OrderResponse> cancel(@PathVariable UUID id,
                                             @RequestBody(required = false) CancelOrderRequest request) {
        return ApiResponse.of("Order cancelled",
                orderService.cancel(id, request == null ? null : request.reason()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Advance an order through fulfilment (admin)")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.of("Order status updated",
                orderService.advance(id, request.status(), request.note()));
    }
}
