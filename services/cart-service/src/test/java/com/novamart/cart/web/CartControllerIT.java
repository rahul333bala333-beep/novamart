package com.novamart.cart.web;

import com.novamart.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cart access control.
 *
 * The service depends on product-service and inventory-service for names, prices
 * and stock, and neither is running in this test — the `test` profile points
 * them at a dead port. That is deliberate: what is asserted here is the
 * behaviour cart-service owns by itself, which is who may reach a cart at all.
 * The enrichment path is covered by the end-to-end suite with real services.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    private String tokenFor(UUID userId) {
        return jwt.issueAccessToken(userId, "shopper@example.test", Set.of("USER"));
    }

    @Test
    void anAnonymousCallerCannotSeeACart() throws Exception {
        mvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void anAnonymousCallerCannotAddToACart() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .contentType("application/json")
                        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anEmptyCartIsAnEmptyCartAndNotA404() throws Exception {
        // Returning 404 for a shopper who has never added anything would force
        // every caller to special-case a perfectly normal state.
        mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + tokenFor(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalQuantity").value(0));
    }

    @Test
    void clearingAnEmptyCartSucceedsQuietly() throws Exception {
        mvc.perform(delete("/api/v1/cart").header("Authorization", "Bearer " + tokenFor(UUID.randomUUID())))
                .andExpect(status().isNoContent());
    }

    @Test
    void internalEndpointsRejectAShopperToken() throws Exception {
        // These exist for order-service. A shopper token must not reach them
        // even though they sit on the same port.
        UUID victim = UUID.randomUUID();
        mvc.perform(get("/api/v1/internal/carts/" + victim)
                        .header("Authorization", "Bearer " + tokenFor(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalEndpointsAcceptTheServiceToken() throws Exception {
        mvc.perform(get("/api/v1/internal/carts/" + UUID.randomUUID())
                        .header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk());
    }

    @Test
    void aWrongInternalTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/internal/carts/" + UUID.randomUUID())
                        .header("X-Internal-Token", "not-the-right-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidQuantitiesAreRejectedBeforeAnyDownstreamCall() throws Exception {
        String token = tokenFor(UUID.randomUUID());

        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMalformedProductIdIsRejected() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + tokenFor(UUID.randomUUID()))
                        .contentType("application/json")
                        .content("{\"productId\":\"not-a-uuid\",\"quantity\":1}"))
                .andExpect(status().isBadRequest());
    }
}
