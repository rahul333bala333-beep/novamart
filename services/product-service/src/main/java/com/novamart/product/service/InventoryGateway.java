package com.novamart.product.service;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.client.ServiceClientFactory;
import com.novamart.product.dto.ProductDtos.AvailabilityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The catalogue's window onto inventory-service.
 *
 * <p>Both calls degrade rather than propagate. A product page is still useful
 * without a live stock badge, and a newly created product that failed to get a
 * stock row simply reads as out of stock until an administrator sets one. In
 * neither case is failing the caller's request the better outcome.
 */
@Component
public class InventoryGateway {

    private static final Logger log = LoggerFactory.getLogger(InventoryGateway.class);

    private final RestClient client;

    public InventoryGateway(ServiceClientFactory factory,
                            @Value("${novamart.services.inventory-url}") String baseUrl) {
        this.client = factory.create(baseUrl, "inventory");
    }

    public Optional<AvailabilityResponse> availabilityOf(UUID productId) {
        try {
            ApiResponse<Map<String, Object>> response = client.get()
                    .uri("/api/v1/inventory/{productId}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || response.data() == null) {
                return Optional.empty();
            }
            Map<String, Object> data = response.data();
            int available = ((Number) data.getOrDefault("availableQuantity", 0)).intValue();
            boolean inStock = Boolean.TRUE.equals(data.get("inStock"));
            return Optional.of(new AvailabilityResponse(available, inStock));
        } catch (RuntimeException ex) {
            log.warn("Stock lookup for product {} failed: {}", productId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void initialiseStock(UUID productId, int quantity) {
        try {
            client.put()
                    .uri("/api/v1/inventory/{productId}", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("totalQuantity", quantity, "reorderThreshold", 5))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Could not seed stock for new product {}: {}", productId, ex.getMessage());
        }
    }
}
