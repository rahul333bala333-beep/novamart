package com.novamart.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A sellable item in the catalogue.
 *
 * <p>Money is {@link BigDecimal} mapped to {@code NUMERIC(12,2)}. A {@code double}
 * cannot represent 0.1 exactly, so totting up prices in floating point drifts by
 * paise and eventually produces an order total that does not equal the sum of its
 * lines. That class of bug is invisible in testing and infuriating in production.
 *
 * <p>Both associations are {@code LAZY}. A catalogue page renders 12 products and
 * needs each product's category name; with eager loading that is 12 extra
 * selects, so the listing query fetches them with an entity graph instead. Making
 * the default eager would hide the cost everywhere else it is not wanted.
 */
@Entity
@Table(name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_products_sku", columnNames = "sku"),
                @UniqueConstraint(name = "uq_products_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_products_category", columnList = "category_id"),
                @Index(name = "idx_products_brand", columnList = "brand_id"),
                @Index(name = "idx_products_active_created", columnList = "active, created_at")
        })
public class Product {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, length = 40)
    private String sku;

    @Column(name = "slug", nullable = false, length = 220)
    private String slug;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "short_description", length = 300)
    private String shortDescription;

    @Column(name = "description", nullable = false, length = 5000)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** The pre-discount price. Null means the item is not on offer. */
    @Column(name = "compare_at_price", precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", foreignKey = @ForeignKey(name = "fk_products_brand"))
    private Brand brand;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    /**
     * Soft-delete flag. Deleting a product row outright would orphan the lines of
     * every order that ever contained it, so removal only hides it from the
     * catalogue.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<ProductSpecification> specifications = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public static Product create(String sku, String slug, String name, Category category) {
        Product product = new Product();
        product.id = UUID.randomUUID();
        product.sku = sku;
        product.slug = slug;
        product.name = name;
        product.category = category;
        product.createdAt = Instant.now();
        product.updatedAt = product.createdAt;
        return product;
    }

    public void applyDetails(String name, String slug, String shortDescription, String description,
                             BigDecimal price, BigDecimal compareAtPrice, Category category,
                             Brand brand, String imageUrl, boolean featured, boolean active) {
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.price = price;
        this.compareAtPrice = compareAtPrice;
        this.category = category;
        this.brand = brand;
        if (imageUrl != null && !imageUrl.isBlank()) {
            this.imageUrl = imageUrl;
        } else if (this.imageUrl == null || this.imageUrl.isBlank()) {
            this.imageUrl = "/uploads/products/placeholder.webp";
        }
        this.featured = featured;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = (imageUrl != null && !imageUrl.isBlank()) ? imageUrl : "/uploads/products/placeholder.webp";
        this.updatedAt = Instant.now();
    }

    public void replaceImages(List<String> urls) {
        images.clear();
        for (int i = 0; i < urls.size(); i++) {
            images.add(ProductImage.create(this, urls.get(i), i));
        }
    }

    public void replaceSpecifications(List<ProductSpecification.Pair> pairs) {
        specifications.clear();
        for (int i = 0; i < pairs.size(); i++) {
            ProductSpecification.Pair pair = pairs.get(i);
            specifications.add(ProductSpecification.create(this, pair.label(), pair.value(), i));
        }
    }

    public void updateRating(BigDecimal average, int count) {
        this.ratingAverage = average != null ? average : BigDecimal.ZERO;
        this.ratingCount = count;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Discount as a whole percentage, or null when the product is not on offer.
     * Derived rather than stored: a stored copy would silently go stale the first
     * time someone edited the price without recalculating it.
     */
    public Integer discountPercent() {
        if (compareAtPrice == null || price == null
                || compareAtPrice.compareTo(price) <= 0
                || compareAtPrice.signum() == 0) {
            return null;
        }
        return compareAtPrice.subtract(price)
                .multiply(BigDecimal.valueOf(100))
                .divide(compareAtPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getCompareAtPrice() {
        return compareAtPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public Category getCategory() {
        return category;
    }

    public Brand getBrand() {
        return brand;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public boolean isFeatured() {
        return featured;
    }

    public boolean isActive() {
        return active;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public List<ProductSpecification> getSpecifications() {
        return specifications;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
