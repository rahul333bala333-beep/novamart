package com.novamart.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The routing table: which path prefix belongs to which service.
 *
 * <p>Defined in Java rather than in {@code application.yml} on purpose. The
 * property namespace for Spring Cloud Gateway routes has been renamed between
 * major versions; the {@code RouteLocatorBuilder} API has not. Java also gets
 * compile-time checking and somewhere to write down why a route exists.
 *
 * <p>Paths are <b>not</b> rewritten. Each service publishes the same
 * {@code /api/v1/...} path the contract documents, so a URL means the same thing
 * whether it arrives through the gateway or is called directly during debugging.
 * Rewriting would make the gateway a translation layer that has to be consulted
 * to understand any URL.
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, Environment env) {
        String auth = env.getProperty("novamart.services.auth-url", "http://localhost:8081");
        String product = env.getProperty("novamart.services.product-url", "http://localhost:8082");
        String cart = env.getProperty("novamart.services.cart-url", "http://localhost:8083");
        String order = env.getProperty("novamart.services.order-url", "http://localhost:8084");
        String payment = env.getProperty("novamart.services.payment-url", "http://localhost:8085");
        String inventory = env.getProperty("novamart.services.inventory-url", "http://localhost:8086");
        String notification = env.getProperty("novamart.services.notification-url", "http://localhost:8087");

        return builder.routes()

                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**")
                        .uri(auth))

                .route("product-service", r -> r
                        .path("/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**", "/api/v1/reviews/**", "/uploads/**")
                        .uri(product))

                .route("cart-service", r -> r
                        .path("/api/v1/cart/**", "/api/v1/wishlist/**")
                        .uri(cart))

                .route("order-service", r -> r
                        .path("/api/v1/orders/**", "/api/v1/coupons/**")
                        .uri(order))

                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .uri(payment))

                .route("inventory-service", r -> r
                        .path("/api/v1/inventory/**")
                        .uri(inventory))

                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .uri(notification))

                .build();
    }
}
