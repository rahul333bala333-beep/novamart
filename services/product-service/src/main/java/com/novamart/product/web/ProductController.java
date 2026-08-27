package com.novamart.product.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.api.PageResponse;
import com.novamart.product.dto.ProductDtos.BatchLookupRequest;
import com.novamart.product.dto.ProductDtos.ProductDetailResponse;
import com.novamart.product.dto.ProductDtos.ProductImageUploadResponse;
import com.novamart.product.dto.ProductDtos.ProductRequest;
import com.novamart.product.dto.ProductDtos.ProductResponse;
import com.novamart.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Validated
@Tag(name = "Products", description = "Catalogue browsing and administration")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Browse the catalogue")
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal minRating,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        return ApiResponse.of("Products retrieved",
                productService.search(search, category, brand, minPrice, maxPrice, featured, minRating, page, size, sort));
    }

    @GetMapping("/{idOrSlug}")
    @SecurityRequirements
    @Operation(summary = "Get a single product by id or slug")
    public ApiResponse<ProductDetailResponse> get(@PathVariable String idOrSlug) {
        return ApiResponse.of("Product retrieved", productService.detail(idOrSlug));
    }

    @PostMapping("/batch")
    @SecurityRequirements
    @Operation(summary = "Resolve many products at once")
    public ApiResponse<List<ProductResponse>> batch(@Valid @RequestBody BatchLookupRequest request) {
        return ApiResponse.of("Products retrieved", productService.byIds(request.productIds()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Product created", productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product (admin)")
    public ApiResponse<ProductResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody ProductRequest request) {
        return ApiResponse.of("Product updated", productService.update(id, request));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload product image from local device (admin)")
    public ApiResponse<ProductImageUploadResponse> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.of("Product image uploaded successfully", productService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product (admin)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
