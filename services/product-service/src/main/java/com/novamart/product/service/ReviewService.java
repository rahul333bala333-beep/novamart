package com.novamart.product.service;

import com.novamart.common.api.PageResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.product.domain.Product;
import com.novamart.product.domain.ProductReview;
import com.novamart.product.dto.ReviewDtos.CreateReviewRequest;
import com.novamart.product.dto.ReviewDtos.ProductReviewSummaryResponse;
import com.novamart.product.dto.ReviewDtos.ReviewResponse;
import com.novamart.product.dto.ReviewDtos.UpdateReviewRequest;
import com.novamart.product.repository.ProductRepository;
import com.novamart.product.repository.ProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ProductReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsForProduct(UUID productId, Pageable pageable) {
        ensureProductExists(productId);
        Page<ProductReview> page = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getReviewSummary(UUID productId) {
        Product product = ensureProductExists(productId);
        List<ProductReview> all = reviewRepository.findByProductId(productId);

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        for (ProductReview r : all) {
            distribution.put(r.getRating(), distribution.getOrDefault(r.getRating(), 0L) + 1);
        }

        return new ProductReviewSummaryResponse(
                productId,
                product.getRatingAverage(),
                product.getRatingCount(),
                distribution);
    }

    @Transactional
    public ReviewResponse createReview(UUID productId, UUID userId, String userName, CreateReviewRequest request) {
        Product product = ensureProductExists(productId);

        // Check if user already reviewed this product
        ProductReview review = reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(existing -> {
                    existing.update(request.rating(), request.title(), request.comment());
                    return existing;
                })
                .orElseGet(() -> ProductReview.create(
                        productId,
                        userId,
                        (userName != null && !userName.isBlank()) ? userName : "Verified Customer",
                        request.rating(),
                        request.title(),
                        request.comment(),
                        true));

        review = reviewRepository.save(review);
        recalculateProductRating(product);
        log.info("Saved review for product {} by user {}", productId, userId);
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UUID userId, UpdateReviewRequest request) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Review not found"));

        if (!review.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "You can only update your own review");
        }

        review.update(request.rating(), request.title(), request.comment());
        review = reviewRepository.save(review);

        Product product = productRepository.findById(review.getProductId()).orElse(null);
        if (product != null) {
            recalculateProductRating(product);
        }

        return toResponse(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId, UUID userId, boolean isAdmin) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Review not found"));

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "You can only delete your own review");
        }

        UUID productId = review.getProductId();
        reviewRepository.delete(review);

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            recalculateProductRating(product);
        }
        log.info("Deleted review {} for product {}", reviewId, productId);
    }

    private void recalculateProductRating(Product product) {
        Double avg = reviewRepository.calculateAverageRating(product.getId());
        long count = reviewRepository.countByProductId(product.getId());

        BigDecimal ratingAvg = avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        product.updateRating(ratingAvg, (int) count);
        productRepository.save(product);
    }

    private Product ensureProductExists(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ReviewResponse toResponse(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getUserName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
