package com.novamart.cart.web;

import com.novamart.cart.dto.CartDtos.CartResponse;
import com.novamart.cart.service.CartService;
import com.novamart.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Cart access for order-service during checkout.
 *
 * <p>order-service needs to read the cart it is turning into an order and empty
 * it once the order is confirmed. It cannot reach {@code cart_db}, so it asks
 * over HTTP. Restricted to the SERVICE role, which only the internal token
 * grants and which the gateway never forwards from outside.
 */
@RestController
@RequestMapping("/api/v1/internal/carts")
@PreAuthorize("hasRole('SERVICE')")
@Tag(name = "Internal", description = "Service-to-service only")
public class InternalCartController {

    private final CartService cartService;

    public InternalCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Read a user's cart during checkout")
    public ApiResponse<CartResponse> get(@PathVariable UUID userId) {
        return ApiResponse.of("Cart retrieved", cartService.getOrCreate(userId));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Empty a user's cart after a confirmed order")
    public ResponseEntity<Void> clear(@PathVariable UUID userId) {
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
