package com.novamart.product.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalogue API against the seeded demo data.
 *
 * The seed is part of the contract for a demonstration project: if a migration
 * broke it, the storefront would be empty and every screenshot in the README
 * would be wrong.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
/**
 * Each test runs in a transaction that is rolled back afterwards.
 *
 * Without this the tests are order-dependent: one that creates a product makes
 * another that asserts "the catalogue has 25 items" fail, and which one runs
 * first is not guaranteed. Rolling back means every test sees the seeded data
 * exactly as the migrations left it, whatever ran before.
 */
class ProductControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    // ---------------------------------------------------------- reading --

    @Test
    @DisplayName("browsing works without signing in")
    void catalogueIsPublic() throws Exception {
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").value(25));
    }

    @Test
    void paginationReportsConsistentMetadata() throws Exception {
        mvc.perform(get("/api/v1/products").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(10))
                .andExpect(jsonPath("$.data.page.totalPages").value(3))
                .andExpect(jsonPath("$.data.page.first").value(true))
                .andExpect(jsonPath("$.data.page.last").value(false));

        mvc.perform(get("/api/v1/products").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.page.last").value(true));
    }

    @Test
    void categoryFilterNarrowsTheResults() throws Exception {
        mvc.perform(get("/api/v1/products").param("category", "audio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(4))
                .andExpect(jsonPath("$.data.content[0].categorySlug").value("audio"));
    }

    @Test
    void priceFilterExcludesOutOfRangeProducts() throws Exception {
        mvc.perform(get("/api/v1/products")
                        .param("minPrice", "5000").param("maxPrice", "15000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.price < 5000)]").isEmpty())
                .andExpect(jsonPath("$.data.content[?(@.price > 15000)]").isEmpty());
    }

    @Test
    void searchMatchesNameCaseInsensitively() throws Exception {
        mvc.perform(get("/api/v1/products").param("search", "ESPRESSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @DisplayName("an unrecognised sort key falls back rather than failing")
    void unknownSortIsIgnored() throws Exception {
        // A stale bookmark carrying an old sort key should still show products.
        mvc.perform(get("/api/v1/products").param("sort", "price;DROP TABLE products--,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void aProductIsRetrievableBySlug() throws Exception {
        mvc.perform(get("/api/v1/products/aurelia-halo-noise-cancelling-headphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.sku").value("AUR-HALO-BLK"))
                .andExpect(jsonPath("$.data.specifications").isArray())
                .andExpect(jsonPath("$.data.images").isArray());
    }

    @Test
    void anUnknownProductReturns404WithACode() throws Exception {
        mvc.perform(get("/api/v1/products/no-such-product"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void discountPercentIsDerivedFromThePrices() throws Exception {
        // 18999 against 24999 is 24% after rounding.
        mvc.perform(get("/api/v1/products/aurelia-halo-noise-cancelling-headphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.discountPercent").value(24));
    }

    @Test
    void categoriesCarryLiveProductCounts() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[?(@.slug=='audio')].productCount").value(4));
    }

    // --------------------------------------------------- authorisation ---

    @Test
    void creatingAProductAnonymouslyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("sku", "X"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("a shopper is refused before the request body is even validated")
    void creatingAProductAsAShopperIsForbidden() throws Exception {
        // The filter chain decides this, so an unauthorised caller learns nothing
        // about which fields an admin payload requires.
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("sku", "X"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdminCanCreateAProduct() throws Exception {
        String categories = mvc.perform(get("/api/v1/categories"))
                .andReturn().getResponse().getContentAsString();
        String categoryId = json.readTree(categories).path("data").get(0).path("id").asText();

        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "sku", "TEST-NEW-001",
                                "name", "Integration Test Product",
                                "description", "Created by an integration test to prove the write path works.",
                                "price", 1499.00,
                                "categoryId", categoryId,
                                "imageUrl", "https://example.test/product.jpg"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku").value("TEST-NEW-001"))
                // The slug is derived from the name, not supplied by the client.
                .andExpect(jsonPath("$.data.slug").value("integration-test-product"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("regression: a product with no brand still appears in the catalogue")
    void aBrandlessProductIsNotSwallowedByTheJoin() throws Exception {
        // Found by running against real PostgreSQL. Referencing `p.brand.slug`
        // in the WHERE clause made Hibernate emit an INNER join to brands, so
        // every product with a null brand_id vanished from the catalogue
        // entirely. All 25 seeded products have a brand, which is why it stayed
        // hidden. `brandId` is nullable in ProductRequest, so an administrator
        // really could create a product that never appeared in the shop.
        String categories = mvc.perform(get("/api/v1/categories"))
                .andReturn().getResponse().getContentAsString();
        String categoryId = json.readTree(categories).path("data").get(0).path("id").asText();

        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "sku", "NO-BRAND-001",
                                "name", "Unbranded Regression Product",
                                "description", "Deliberately created without a brand to guard the LEFT JOIN.",
                                "price", 499.00,
                                "categoryId", categoryId,
                                "imageUrl", "https://example.test/unbranded.jpg"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.brandId").doesNotExist());

        // It must be findable by search...
        mvc.perform(get("/api/v1/products").param("search", "Unbranded Regression"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(1));

        // ...and by its slug.
        mvc.perform(get("/api/v1/products/unbranded-regression-product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.sku").value("NO-BRAND-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aDuplicateSkuIsRejectedWith409() throws Exception {
        String categories = mvc.perform(get("/api/v1/categories"))
                .andReturn().getResponse().getContentAsString();
        String categoryId = json.readTree(categories).path("data").get(0).path("id").asText();

        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "sku", "AUR-HALO-BLK",
                                "name", "Duplicate SKU Product",
                                "description", "This SKU already exists in the seeded catalogue.",
                                "price", 999.00,
                                "categoryId", categoryId,
                                "imageUrl", "https://example.test/product.jpg"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SKU_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidProductDataReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "sku", "A",
                                "name", "B",
                                "description", "too short",
                                "price", -5,
                                "categoryId", "11111111-1111-1111-1111-111111111111",
                                "imageUrl", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
