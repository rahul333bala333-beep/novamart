package com.novamart.cart.web;

import com.novamart.cart.dto.WishlistDtos.AddWishlistItemRequest;
import com.novamart.cart.dto.WishlistDtos.WishlistResponse;
import com.novamart.cart.service.WishlistService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "Wishlist", description = "The signed-in shopper's wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @Operation(summary = "Get the caller's wishlist")
    public ApiResponse<WishlistResponse> get() {
        return ApiResponse.of("Wishlist retrieved", wishlistService.getWishlist(CurrentUser.requireId()));
    }

    @PostMapping
    @Operation(summary = "Add a product to the caller's wishlist")
    public ApiResponse<WishlistResponse> add(@Valid @RequestBody AddWishlistItemRequest request) {
        return ApiResponse.of("Item added to wishlist",
                wishlistService.addItem(CurrentUser.requireId(), request.productId()));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a product from the caller's wishlist")
    public ApiResponse<WishlistResponse> remove(@PathVariable UUID productId) {
        return ApiResponse.of("Item removed from wishlist",
                wishlistService.removeItem(CurrentUser.requireId(), productId));
    }

    @PostMapping("/{productId}/move-to-cart")
    @Operation(summary = "Move a wishlist item directly into the shopping cart")
    public ResponseEntity<ApiResponse<String>> moveToCart(@PathVariable UUID productId) {
        wishlistService.moveToCart(CurrentUser.requireId(), productId);
        return ResponseEntity.ok(ApiResponse.of("Item moved to cart", "SUCCESS"));
    }
}
