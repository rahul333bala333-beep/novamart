package com.novamart.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS, configured once for the whole platform.
 *
 * <p>The browser only ever talks to the gateway, so this is the only place an
 * origin allow-list is needed. Repeating it in seven services would create seven
 * chances to get it wrong and one guarantee that they eventually disagree.
 *
 * <p>Origins come from configuration and are an explicit list. A wildcard would
 * be simpler and is the usual shortcut, but it cannot be combined with
 * credentials and it means any site on the internet can call this API with a
 * victim's browser.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${novamart.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key",
                "X-Correlation-Id", "Accept"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        // Tokens travel in the Authorization header, not in cookies, so the
        // browser never needs to attach credentials automatically.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsWebFilter(source);
    }
}
