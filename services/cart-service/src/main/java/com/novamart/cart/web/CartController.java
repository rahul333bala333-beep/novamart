package com.novamart.cart.web;

import com.novamart.cart.dto.CartDtos.AddCartItemRequest;
import com.novamart.cart.dto.CartDtos.CartResponse;
import com.novamart.cart.dto.CartDtos.UpdateCartItemRequest;
import com.novamart.cart.service.CartService;
import com.novamart.common.api.ApiResponse;
import com.novamart.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The cart of whoever is signed in.
 *
 * <p>No endpoint here accepts a user id. The cart is always the caller's own,
 * taken from the verified token, so there is no parameter to tamper with.
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "The signed-in shopper's cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get the caller's cart")
    public ApiResponse<CartResponse> get() {
        return ApiResponse.of("Cart retrieved", cartService.getOrCreate(CurrentUser.requireId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart")
    public ApiResponse<CartResponse> add(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.of("Item added to cart",
                cartService.addItem(CurrentUser.requireId(), request.productId(), request.quantity()));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Set the quantity of a cart line")
    public ApiResponse<CartResponse> update(@PathVariable UUID productId,
                                            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.of("Cart updated",
                cartService.updateItem(CurrentUser.requireId(), productId, request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a line from the cart")
    public ApiResponse<CartResponse> remove(@PathVariable UUID productId) {
        return ApiResponse.of("Item removed",
                cartService.removeItem(CurrentUser.requireId(), productId));
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ResponseEntity<Void> clear() {
        cartService.clear(CurrentUser.requireId());
        return ResponseEntity.noContent().build();
    }
}
