package com.novamart.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novamart.common.client.InternalClientProperties;
import com.novamart.common.error.GlobalExceptionHandler;
import com.novamart.common.security.JwtAuthenticationFilter;
import com.novamart.common.security.JwtService;
import com.novamart.common.security.RestAuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Servlet-only wiring: the exception handler, the authentication filter and the
 * JSON 401/403 renderer.
 *
 * <p>Guarded by {@code @ConditionalOnWebApplication(SERVLET)} so that the
 * reactive API gateway, which shares this library for JWT verification, does not
 * try to load servlet classes it has no stack for.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
public class NovaMartWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                           ObjectMapper objectMapper,
                                                           InternalClientProperties internalProperties) {
        return new JwtAuthenticationFilter(jwtService, objectMapper, internalProperties.token());
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }
}
