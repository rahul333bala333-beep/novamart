package com.novamart.product.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.api.PageResponse;
import com.novamart.common.security.CurrentUser;
import com.novamart.product.dto.ReviewDtos.CreateReviewRequest;
import com.novamart.product.dto.ReviewDtos.ProductReviewSummaryResponse;
import com.novamart.product.dto.ReviewDtos.ReviewResponse;
import com.novamart.product.dto.ReviewDtos.UpdateReviewRequest;
import com.novamart.product.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Reviews", description = "Product ratings and reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/v1/products/{productId}/reviews")
    @Operation(summary = "List customer reviews for a product")
    public PageResponse<ReviewResponse> listByProduct(@PathVariable UUID productId,
                                                      @PageableDefault(size = 10) Pageable pageable) {
        return reviewService.getReviewsForProduct(productId, pageable);
    }

    @GetMapping("/api/v1/products/{productId}/reviews/summary")
    @Operation(summary = "Get review distribution and summary statistics for a product")
    public ApiResponse<ProductReviewSummaryResponse> getSummary(@PathVariable UUID productId) {
        return ApiResponse.of("Review summary", reviewService.getReviewSummary(productId));
    }

    @PostMapping("/api/v1/products/{productId}/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a rating and review for a product")
    public ApiResponse<ReviewResponse> create(@PathVariable UUID productId,
                                              @Valid @RequestBody CreateReviewRequest request) {
        String userName = CurrentUser.require().email();
        return ApiResponse.of("Review submitted successfully",
                reviewService.createReview(productId, CurrentUser.requireId(), userName, request));
    }

    @PutMapping("/api/v1/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a review")
    public ApiResponse<ReviewResponse> update(@PathVariable UUID id,
                                              @Valid @RequestBody UpdateReviewRequest request) {
        return ApiResponse.of("Review updated successfully",
                reviewService.updateReview(id, CurrentUser.requireId(), request));
    }

    @DeleteMapping("/api/v1/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID id) {
        boolean isAdmin = CurrentUser.require().isAdmin();
        reviewService.deleteReview(id, CurrentUser.requireId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.of("Review deleted", "SUCCESS"));
    }
}
