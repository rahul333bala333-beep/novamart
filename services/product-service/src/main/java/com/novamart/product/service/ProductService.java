package com.novamart.product.service;

import com.novamart.common.api.PageResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.product.domain.Brand;
import com.novamart.product.domain.Category;
import com.novamart.product.domain.Product;
import com.novamart.product.domain.ProductImage;
import com.novamart.product.domain.ProductSpecification;
import com.novamart.product.dto.ProductDtos.AvailabilityResponse;
import com.novamart.product.dto.ProductDtos.ProductDetailResponse;
import com.novamart.product.dto.ProductDtos.ProductImageUploadResponse;
import com.novamart.product.dto.ProductDtos.ProductRequest;
import com.novamart.product.dto.ProductDtos.ProductResponse;
import com.novamart.product.dto.ProductDtos.SpecificationDto;
import com.novamart.product.repository.BrandRepository;
import com.novamart.product.repository.CategoryRepository;
import com.novamart.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    private static final Map<String, String> SORTABLE = Map.of(
            "createdAt", "createdAt",
            "price", "price",
            "name", "name",
            "ratingAverage", "ratingAverage");

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final InventoryGateway inventory;
    private final String uploadDir;

    public ProductService(ProductRepository products,
                          CategoryRepository categories,
                          BrandRepository brands,
                          InventoryGateway inventory,
                          @Value("${novamart.upload.dir:uploads}") String uploadDir) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.inventory = inventory;
        this.uploadDir = uploadDir;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, String categorySlug, String brandSlug,
                                                BigDecimal minPrice, BigDecimal maxPrice, Boolean featured,
                                                BigDecimal minRating,
                                                int page, int size, String sort) {
        Page<Product> result = products.search(
                blankToNull(search), blankToNull(categorySlug), blankToNull(brandSlug),
                minPrice, maxPrice, featured, minRating,
                PageRequest.of(page, size, parseSort(sort)));
        return PageResponse.from(result, ProductResponse::from);
    }

    /**
     * Product page payload.
     *
     * <p>Accepts a slug or a UUID so storefront URLs can stay human-readable
     * while service-to-service calls keep using ids.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse detail(String idOrSlug) {
        Product product = findByIdOrSlug(idOrSlug);

        List<String> images = product.getImages().stream().map(ProductImage::getUrl).toList();
        if (images.isEmpty()) {
            images = List.of(product.getImageUrl());
        }
        List<SpecificationDto> specs = product.getSpecifications().stream()
                .map(s -> new SpecificationDto(s.getLabel(), s.getValue()))
                .toList();

        AvailabilityResponse availability = inventory.availabilityOf(product.getId()).orElse(null);

        return new ProductDetailResponse(
                ProductResponse.from(product), product.getDescription(), images, specs, availability);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> byIds(List<UUID> ids) {
        return products.findByIdInAndActiveTrue(ids).stream().map(ProductResponse::from).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (products.existsBySku(request.sku())) {
            throw new ApiException(ErrorCode.SKU_ALREADY_EXISTS);
        }
        Category category = requireCategory(request.categoryId());
        Product product = Product.create(request.sku().trim(), uniqueSlug(request.name()),
                request.name().trim(), category);
        apply(product, request, category);
        products.save(product);

        // Inventory lives in another service and another database, so the stock
        // record is created over HTTP. If that call fails the product still
        // exists but has no stock row, which reads as out of stock rather than
        // as an inconsistency a shopper can act on.
        if (request.initialStock() != null) {
            inventory.initialiseStock(product.getId(), request.initialStock());
        }

        log.info("Created product {} ({})", product.getId(), product.getSku());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        Category category = requireCategory(request.categoryId());

        // Renaming keeps the original slug so existing links and shared URLs do
        // not silently 404.
        apply(product, request, category);
        log.info("Updated product {}", id);
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        product.deactivate();
        log.info("Deactivated product {}", id);
    }

    @Transactional
    public ProductImageUploadResponse uploadImage(UUID productId, MultipartFile file) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));

        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Please select an image file to upload.");
        }

        if (file.getSize() > (5 * 1024 * 1024)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Image size must be less than 5 MB.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = getValidExtension(originalFilename, contentType);

        if (extension == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Please upload a JPG, PNG, or WEBP image.");
        }

        try {
            Path uploadRootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path productsDir = uploadRootDir.resolve("products").normalize();
            if (!productsDir.startsWith(uploadRootDir)) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid upload directory destination.");
            }
            Files.createDirectories(productsDir);

            String safeFilename = UUID.randomUUID() + "." + extension;
            Path targetFile = productsDir.resolve(safeFilename).normalize();
            if (!targetFile.startsWith(productsDir)) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid file path traversal detected.");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String oldImageUrl = product.getImageUrl();
            String persistentPath = "/uploads/products/" + safeFilename;
            product.updateImageUrl(persistentPath);
            products.save(product);

            // Clean up previous uploaded image file if it belonged to this products upload dir
            if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/products/") && !oldImageUrl.contains("placeholder")) {
                try {
                    String oldFileName = oldImageUrl.substring("/uploads/products/".length());
                    Path oldFilePath = productsDir.resolve(oldFileName).normalize();
                    if (oldFilePath.startsWith(productsDir) && Files.exists(oldFilePath)) {
                        Files.deleteIfExists(oldFilePath);
                    }
                } catch (Exception ex) {
                    log.warn("Could not delete old image file for product {}: {}", productId, ex.getMessage());
                }
            }

            log.info("Uploaded product image for {} -> {}", productId, persistentPath);
            return new ProductImageUploadResponse(persistentPath);
        } catch (IOException e) {
            log.error("Failed to save uploaded image for product {}", productId, e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to save uploaded image. Please try again.");
        }
    }

    private String getValidExtension(String originalFilename, String contentType) {
        String ext = null;
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (ext != null && ALLOWED_EXTENSIONS.contains(ext)) {
            return ext.equals("jpeg") ? "jpg" : ext;
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase().trim();
            if (ALLOWED_CONTENT_TYPES.contains(ct)) {
                return switch (ct) {
                    case "image/png" -> "png";
                    case "image/webp" -> "webp";
                    default -> "jpg";
                };
            }
        }
        return null;
    }

    // ---------- internals ----------

    private void apply(Product product, ProductRequest request, Category category) {
        Brand brand = request.brandId() == null ? null
                : brands.findById(request.brandId())
                        .orElseThrow(() -> new ApiException(ErrorCode.BRAND_NOT_FOUND));

        product.applyDetails(
                request.name().trim(),
                product.getSlug() == null ? uniqueSlug(request.name()) : product.getSlug(),
                request.shortDescription(),
                request.description(),
                request.price(),
                request.compareAtPrice(),
                category,
                brand,
                request.imageUrl(),
                request.featured(),
                request.active() == null || request.active());

        if (request.images() != null && !request.images().isEmpty()) {
            product.replaceImages(request.images());
        }
        if (request.specifications() != null) {
            product.replaceSpecifications(request.specifications().stream()
                    .map(s -> new ProductSpecification.Pair(s.label(), s.value()))
                    .toList());
        }
    }

    private Product findByIdOrSlug(String idOrSlug) {
        try {
            UUID id = UUID.fromString(idOrSlug);
            return products.findByIdAndActiveTrue(id)
                    .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        } catch (IllegalArgumentException notAUuid) {
            return products.findBySlugAndActiveTrue(idOrSlug)
                    .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        }
    }

    private Category requireCategory(UUID categoryId) {
        return categories.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private String uniqueSlug(String name) {
        String base = Slugs.of(name);
        String candidate = base;
        int suffix = 2;
        while (products.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private static Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",", 2);
        String field = SORTABLE.get(parts[0].trim());
        if (field == null) {
            // Silently falling back beats a 400 here: a stale bookmark carrying an
            // old sort key should still show the catalogue.
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
