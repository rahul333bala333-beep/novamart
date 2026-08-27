package com.novamart.product.dto;

import com.novamart.product.domain.Brand;
import com.novamart.product.domain.Category;
import com.novamart.product.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response shapes for the catalogue, plus entity-to-DTO mapping. */
public final class ProductDtos {

    private ProductDtos() {
    }

    // ---------- requests ----------

    public record ProductRequest(
            @NotBlank @Size(min = 2, max = 40) String sku,
            @NotBlank @Size(min = 2, max = 180) String name,
            @Size(max = 300) String shortDescription,
            @NotBlank @Size(min = 10, max = 5000) String description,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal compareAtPrice,
            @NotNull UUID categoryId,
            UUID brandId,
            @Size(max = 500) String imageUrl,
            List<String> images,
            List<SpecificationDto> specifications,
            boolean featured,
            Boolean active,
            @Min(0) Integer initialStock) {
    }

    public record ProductImageUploadResponse(
            String imageUrl) {
    }

    public record SpecificationDto(
            @NotBlank @Size(max = 80) String label,
            @NotBlank @Size(max = 300) String value) {
    }

    public record CategoryRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @Size(max = 500) String description,
            @Size(max = 500) String imageUrl) {
    }

    public record BatchLookupRequest(
            @NotNull @Size(min = 1, max = 100) List<UUID> productIds) {
    }

    // ---------- responses ----------

    public record ProductResponse(
            UUID id,
            String sku,
            String slug,
            String name,
            String shortDescription,
            BigDecimal price,
            BigDecimal compareAtPrice,
            Integer discountPercent,
            String currency,
            UUID categoryId,
            String categoryName,
            String categorySlug,
            UUID brandId,
            String brandName,
            String imageUrl,
            BigDecimal ratingAverage,
            int ratingCount,
            boolean featured,
            boolean active,
            Instant createdAt) {

        public static ProductResponse from(Product p) {
            Brand brand = p.getBrand();
            Category category = p.getCategory();
            return new ProductResponse(
                    p.getId(), p.getSku(), p.getSlug(), p.getName(), p.getShortDescription(),
                    p.getPrice(), p.getCompareAtPrice(), p.discountPercent(), p.getCurrency(),
                    category.getId(), category.getName(), category.getSlug(),
                    brand == null ? null : brand.getId(),
                    brand == null ? null : brand.getName(),
                    p.getImageUrl(), p.getRatingAverage(), p.getRatingCount(),
                    p.isFeatured(), p.isActive(), p.getCreatedAt());
        }
    }

    /**
     * The product page payload.
     *
     * <p>{@code availability} is nullable on purpose. It is owned by
     * inventory-service, so if that service is briefly unreachable the product
     * page still renders with everything the catalogue knows, and only the stock
     * badge degrades. Failing the whole request because a stock count was
     * unavailable would be a worse outcome for the shopper.
     */
    public record ProductDetailResponse(
            ProductResponse product,
            String description,
            List<String> images,
            List<SpecificationDto> specifications,
            AvailabilityResponse availability) {
    }

    public record AvailabilityResponse(
            int availableQuantity,
            boolean inStock) {
    }

    public record CategoryResponse(
            UUID id,
            String name,
            String slug,
            String description,
            String imageUrl,
            long productCount) {

        public static CategoryResponse from(Category c, long productCount) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(),
                    c.getDescription(), c.getImageUrl(), productCount);
        }
    }

    public record BrandResponse(UUID id, String name, String slug, long productCount) {

        public static BrandResponse from(Brand b, long productCount) {
            return new BrandResponse(b.getId(), b.getName(), b.getSlug(), productCount);
        }
    }
}
