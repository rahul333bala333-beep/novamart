package com.novamart.inventory.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.api.PageResponse;
import com.novamart.inventory.dto.BatchLookupRequest;
import com.novamart.inventory.dto.InventoryDtos.InventoryResponse;
import com.novamart.inventory.dto.InventoryDtos.StockMovementRequest;
import com.novamart.inventory.dto.InventoryDtos.UpdateInventoryRequest;
import com.novamart.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@Validated
@Tag(name = "Inventory", description = "Stock levels, reservations and releases")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    @SecurityRequirements
    @Operation(summary = "Check availability for one product")
    public ApiResponse<InventoryResponse> get(@PathVariable UUID productId) {
        return ApiResponse.of("Stock retrieved", inventoryService.get(productId));
    }

    @PostMapping("/batch")
    @SecurityRequirements
    @Operation(summary = "Check availability for many products at once")
    public ApiResponse<List<InventoryResponse>> batch(@Valid @RequestBody BatchLookupRequest request) {
        return ApiResponse.of("Stock retrieved", inventoryService.byIds(request.productIds()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List stock records (admin)")
    public ApiResponse<PageResponse<InventoryResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean lowStockOnly) {
        return ApiResponse.of("Stock records retrieved",
                inventoryService.list(lowStockOnly, PageRequest.of(page, size)));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @Operation(summary = "Set the on-hand quantity")
    public ApiResponse<InventoryResponse> update(@PathVariable UUID productId,
                                                 @Valid @RequestBody UpdateInventoryRequest request) {
        return ApiResponse.of("Stock updated",
                inventoryService.upsert(productId, request.totalQuantity(), request.reorderThreshold()));
    }

    /*
     * The three movement endpoints are restricted to SERVICE and ADMIN. A shopper
     * must never be able to reserve or release stock directly: reservations are a
     * consequence of placing an order, and exposing them would let anyone empty
     * the warehouse from a browser without buying anything.
     */

    @PostMapping("/{productId}/reserve")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @Operation(summary = "Hold stock for an in-flight order")
    public ApiResponse<InventoryResponse> reserve(@PathVariable UUID productId,
                                                  @Valid @RequestBody StockMovementRequest request) {
        return ApiResponse.of("Stock reserved",
                inventoryService.reserve(productId, request.quantity(), request.referenceId()));
    }

    @PostMapping("/{productId}/release")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @Operation(summary = "Return reserved stock to the available pool")
    public ApiResponse<InventoryResponse> release(@PathVariable UUID productId,
                                                  @Valid @RequestBody StockMovementRequest request) {
        return ApiResponse.of("Stock released",
                inventoryService.release(productId, request.quantity(), request.referenceId()));
    }

    @PostMapping("/{productId}/commit")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @Operation(summary = "Consume a reservation once payment has settled")
    public ApiResponse<InventoryResponse> commit(@PathVariable UUID productId,
                                                 @Valid @RequestBody StockMovementRequest request) {
        return ApiResponse.of("Reservation committed",
                inventoryService.commit(productId, request.quantity(), request.referenceId()));
    }
}
