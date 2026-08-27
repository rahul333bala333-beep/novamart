package com.novamart.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uq_reviews_user_product", columnNames = {"user_id", "product_id"}),
        indexes = {
                @Index(name = "idx_reviews_product", columnList = "product_id"),
                @Index(name = "idx_reviews_user", columnList = "user_id")
        })
public class ProductReview {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false, length = 120)
    private String userName;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "comment", nullable = false, length = 3000)
    private String comment;

    @Column(name = "verified_purchase", nullable = false)
    private boolean verifiedPurchase = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductReview() {
    }

    public static ProductReview create(UUID productId, UUID userId, String userName, int rating, String title, String comment, boolean verifiedPurchase) {
        ProductReview review = new ProductReview();
        review.id = UUID.randomUUID();
        review.productId = productId;
        review.userId = userId;
        review.userName = userName;
        review.rating = rating;
        review.title = title;
        review.comment = comment;
        review.verifiedPurchase = verifiedPurchase;
        review.createdAt = Instant.now();
        review.updatedAt = review.createdAt;
        return review;
    }

    public void update(int rating, String title, String comment) {
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public int getRating() {
        return rating;
    }

    public String getTitle() {
        return title;
    }

    public String getComment() {
        return comment;
    }

    public boolean isVerifiedPurchase() {
        return verifiedPurchase;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
