package com.novamart.product.config;

import com.novamart.common.security.JwtAuthenticationFilter;
import com.novamart.common.security.RestAuthenticationEntryPoint;
import com.novamart.common.web.ApiSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           RestAuthenticationEntryPoint entryPoint) throws Exception {
        ApiSecurity.applyDefaults(http, jwtFilter, entryPoint)
                .authorizeHttpRequests(auth -> auth
                        // Browsing the catalogue must work before anyone signs in;
                        // a shop that demands a login to show its products has no
                        // way of acquiring customers.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**",
                                "/api/v1/categories",
                                "/api/v1/brands",
                                "/uploads/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Batch product resolution is public / open to all callers (storefront, cart-service, shoppers)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/batch").permitAll()
                        // Catalogue writes are gated here, in the filter chain, as
                        // well as by @PreAuthorize on each handler.
                        .requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/*/image", "/api/v1/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated());
        return http.build();
    }
}
