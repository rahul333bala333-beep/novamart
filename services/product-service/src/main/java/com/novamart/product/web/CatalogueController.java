package com.novamart.product.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.product.dto.ProductDtos.BrandResponse;
import com.novamart.product.dto.ProductDtos.CategoryRequest;
import com.novamart.product.dto.ProductDtos.CategoryResponse;
import com.novamart.product.service.CatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Categories", description = "Category taxonomy and brand directory")
public class CatalogueController {

    private final CatalogueService catalogueService;

    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping("/api/v1/categories")
    @SecurityRequirements
    @Operation(summary = "List categories with live product counts")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.of("Categories retrieved", catalogueService.listCategories());
    }

    @PostMapping("/api/v1/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category (admin)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Category created", catalogueService.createCategory(request)));
    }

    @PutMapping("/api/v1/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category (admin)")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable UUID id,
                                                        @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.of("Category updated", catalogueService.updateCategory(id, request));
    }

    @DeleteMapping("/api/v1/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category (admin)")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        catalogueService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/brands")
    @SecurityRequirements
    @Operation(summary = "List brands")
    public ApiResponse<List<BrandResponse>> brands() {
        return ApiResponse.of("Brands retrieved", catalogueService.listBrands());
    }
}
