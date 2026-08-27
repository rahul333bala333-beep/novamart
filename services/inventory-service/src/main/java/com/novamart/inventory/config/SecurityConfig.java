package com.novamart.inventory.config;

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
                        // Reading availability is public so a product page can show
                        // a stock badge to a signed-out visitor. Every mutation is
                        // gated by @PreAuthorize on the handler.
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/batch").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
