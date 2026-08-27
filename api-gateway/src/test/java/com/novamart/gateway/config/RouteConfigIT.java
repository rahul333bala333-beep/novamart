package com.novamart.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The routing table, resolved from the real application context.
 *
 * Loading the actual context rather than constructing RouteConfig by hand means
 * the predicate factories, the property binding and the bean wiring are all
 * exercised as they are in production. A hand-built context would have tested a
 * configuration that never runs.
 */
// MOCK (the default) rather than NONE: the gateway registers a reactive
// error handler that needs a web application context to resolve
// ErrorAttributes. NONE would test a context the application never uses.
@SpringBootTest
@ActiveProfiles("test")
class RouteConfigIT {

    @Autowired
    private RouteLocator routes;

    private String routeIdFor(String path) {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        // Gateway predicates are asynchronous, so they are subscribed to rather
        // than called directly.
        List<Route> matched = routes.getRoutes()
                .filterWhen(route -> Mono.from(route.getPredicate().apply(exchange)))
                .collectList()
                .block();
        return Objects.requireNonNull(matched).isEmpty() ? null : matched.get(0).getId();
    }

    @Test
    void everyServiceHasExactlyOneRoute() {
        List<Route> all = routes.getRoutes().collectList().block();
        assertThat(all).extracting(Route::getId).containsExactlyInAnyOrder(
                "auth-service", "product-service", "cart-service", "order-service",
                "payment-service", "inventory-service", "notification-service");
    }

    @Test
    @DisplayName("each path prefix reaches the service that owns it")
    void prefixesRouteToTheOwningService() {
        assertThat(routeIdFor("/api/v1/auth/login")).isEqualTo("auth-service");
        assertThat(routeIdFor("/api/v1/users/me")).isEqualTo("auth-service");
        assertThat(routeIdFor("/api/v1/products")).isEqualTo("product-service");
        assertThat(routeIdFor("/api/v1/categories")).isEqualTo("product-service");
        assertThat(routeIdFor("/api/v1/brands")).isEqualTo("product-service");
        assertThat(routeIdFor("/api/v1/cart")).isEqualTo("cart-service");
        assertThat(routeIdFor("/api/v1/cart/items")).isEqualTo("cart-service");
        assertThat(routeIdFor("/api/v1/orders")).isEqualTo("order-service");
        assertThat(routeIdFor("/api/v1/payments")).isEqualTo("payment-service");
        assertThat(routeIdFor("/api/v1/inventory/abc")).isEqualTo("inventory-service");
        assertThat(routeIdFor("/api/v1/notifications")).isEqualTo("notification-service");
    }

    @Test
    void anUnknownPrefixMatchesNoRoute() {
        assertThat(routeIdFor("/api/v1/unknown")).isNull();
        assertThat(routeIdFor("/")).isNull();
        assertThat(routeIdFor("/actuator/health")).isNull();
    }

    @Test
    @DisplayName("paths are forwarded unrewritten")
    void routesApplyNoPathRewriting() {
        // Each service publishes the same /api/v1/... path the contract
        // documents, so a URL means the same thing whether it arrives through
        // the gateway or is called directly while debugging.
        List<Route> all = routes.getRoutes().collectList().block();
        assertThat(all).allSatisfy(route ->
                assertThat(route.getFilters())
                        .as("route %s should apply no path-rewriting filters", route.getId())
                        .isEmpty());
    }

    @Test
    void routeTargetsComeFromConfiguration() {
        List<Route> all = routes.getRoutes().collectList().block();
        assertThat(all).extracting(route -> route.getUri().toString())
                .contains("http://auth-service:8081", "http://product-service:8082",
                        "http://cart-service:8083", "http://order-service:8084",
                        "http://payment-service:8085", "http://inventory-service:8086",
                        "http://notification-service:8087");
    }
}
