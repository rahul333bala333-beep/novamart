package com.novamart.cart.client;

import com.novamart.cart.dto.CartDtos.ProductSnapshot;
import com.novamart.cart.dto.CartDtos.StockSnapshot;
import com.novamart.common.api.ApiResponse;
import com.novamart.common.client.ServiceClientFactory;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads product and stock data the cart does not own.
 *
 * <p>Both lookups are batched. Fetching a twelve-line cart one product at a time
 * would be an N+1 over the network, where each iteration costs a round trip
 * rather than a local query, so two calls replace twenty-four.
 */
@Component
public class CatalogueGateway {

    private static final Logger log = LoggerFactory.getLogger(CatalogueGateway.class);

    private final RestClient productClient;
    private final RestClient inventoryClient;

    public CatalogueGateway(ServiceClientFactory factory,
                            @Value("${novamart.services.product-url}") String productUrl,
                            @Value("${novamart.services.inventory-url}") String inventoryUrl) {
        this.productClient = factory.create(productUrl, "product");
        this.inventoryClient = factory.create(inventoryUrl, "inventory");
    }

    /** Resolves one product, or fails with PRODUCT_NOT_FOUND if it is gone or inactive. */
    public ProductSnapshot requireProduct(UUID productId) {
        return productsByIds(List.of(productId)).values().stream().findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public Map<UUID, ProductSnapshot> productsByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        ApiResponse<List<Map<String, Object>>> response = productClient.post()
                .uri("/api/v1/products/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("productIds", ids))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.data() == null) {
            return Map.of();
        }
        return response.data().stream()
                .map(CatalogueGateway::toProduct)
                .collect(Collectors.toMap(ProductSnapshot::id, Function.identity(), (a, b) -> a));
    }

    /**
     * Stock for many products.
     *
     * <p>Degrades rather than fails: if inventory-service is unreachable the cart
     * still renders with its prices and totals, and only the availability badge
     * is missing. Stock is re-checked authoritatively at checkout, so a stale
     * badge cannot cause an oversell.
     */
    public Map<UUID, StockSnapshot> stockByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            ApiResponse<List<Map<String, Object>>> response = inventoryClient.post()
                    .uri("/api/v1/inventory/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("productIds", ids))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || response.data() == null) {
                return Map.of();
            }
            return response.data().stream()
                    .map(CatalogueGateway::toStock)
                    .collect(Collectors.toMap(StockSnapshot::productId, Function.identity(), (a, b) -> a));
        } catch (RuntimeException ex) {
            log.warn("Batch stock lookup failed, cart will render without availability: {}", ex.getMessage());
            return Map.of();
        }
    }

    /** Availability for a single product. Used before a line is written. */
    public int availableQuantity(UUID productId) {
        Map<UUID, StockSnapshot> stock = stockByIds(List.of(productId));
        StockSnapshot snapshot = stock.get(productId);
        // Absent stock record means nothing has been stocked yet, which is zero
        // available rather than unlimited.
        return snapshot == null ? 0 : snapshot.availableQuantity();
    }

    private static ProductSnapshot toProduct(Map<String, Object> raw) {
        return new ProductSnapshot(
                UUID.fromString((String) raw.get("id")),
                (String) raw.get("name"),
                (String) raw.get("slug"),
                (String) raw.get("imageUrl"),
                new BigDecimal(String.valueOf(raw.get("price"))),
                (String) raw.getOrDefault("currency", "INR"));
    }

    private static StockSnapshot toStock(Map<String, Object> raw) {
        return new StockSnapshot(
                UUID.fromString((String) raw.get("productId")),
                ((Number) raw.getOrDefault("availableQuantity", 0)).intValue(),
                Boolean.TRUE.equals(raw.get("inStock")));
    }
}
