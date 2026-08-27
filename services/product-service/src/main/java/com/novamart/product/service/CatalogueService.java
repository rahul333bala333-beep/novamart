package com.novamart.product.service;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.product.domain.Category;
import com.novamart.product.dto.ProductDtos.BrandResponse;
import com.novamart.product.dto.ProductDtos.CategoryRequest;
import com.novamart.product.dto.ProductDtos.CategoryResponse;
import com.novamart.product.repository.BrandRepository;
import com.novamart.product.repository.CategoryRepository;
import com.novamart.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Category and brand administration and lookup. */
@Service
public class CatalogueService {

    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;

    public CatalogueService(CategoryRepository categories,
                            BrandRepository brands,
                            ProductRepository products) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        // One grouped count for the whole list rather than a count query per
        // category. With a dozen categories the difference is 2 queries instead
        // of 13, and it does not get worse as the taxonomy grows.
        Map<UUID, Long> counts = toCountMap(products.countActiveByCategory());
        return categories.findAllByOrderByNameAsc().stream()
                .map(c -> CategoryResponse.from(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands() {
        Map<UUID, Long> counts = toCountMap(products.countActiveByBrand());
        return brands.findAllByOrderByNameAsc().stream()
                .map(b -> BrandResponse.from(b, counts.getOrDefault(b.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = Slugs.of(request.name());
        if (categories.existsBySlug(slug)) {
            throw new ApiException(ErrorCode.SLUG_ALREADY_EXISTS,
                    "A category with a similar name already exists");
        }
        Category category = Category.create(request.name().trim(), slug,
                request.description(), request.imageUrl());
        categories.save(category);
        return CategoryResponse.from(category, 0L);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
        category.update(request.name().trim(), category.getSlug(),
                request.description(), request.imageUrl());
        return CategoryResponse.from(category, products.countByCategoryIdAndActiveTrue(id));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));

        // Refusing rather than cascading. Deleting a category that still has
        // products would either orphan them or silently delete stock the
        // administrator did not intend to remove.
        if (products.countByCategoryIdAndActiveTrue(id) > 0) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_EMPTY);
        }
        categories.delete(category);
    }

    private static Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
