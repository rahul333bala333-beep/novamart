package com.novamart.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novamart.common.client.InternalClientProperties;
import com.novamart.common.client.ServiceClientFactory;
import com.novamart.common.security.JwtProperties;
import com.novamart.common.security.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring that every Nova Mart application needs, servlet or reactive.
 *
 * <p>Registered through {@code AutoConfiguration.imports} rather than by asking
 * each service to widen its component scan to {@code com.novamart.common}. A
 * shared library that requires its consumers to know its package layout is a
 * library that will eventually be wired up wrong; auto-configuration makes
 * adding the dependency sufficient.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, InternalClientProperties.class})
public class NovaMartCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceClientFactory serviceClientFactory(InternalClientProperties properties,
                                                     ObjectMapper objectMapper) {
        return new ServiceClientFactory(properties, objectMapper);
    }
}
