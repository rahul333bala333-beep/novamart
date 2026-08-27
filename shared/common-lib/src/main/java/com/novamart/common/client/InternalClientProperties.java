package com.novamart.common.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for outbound service-to-service HTTP, bound from {@code novamart.internal.*}.
 *
 * @param token          shared credential presented as {@code X-Internal-Token}
 * @param connectTimeout how long to wait for a TCP connection
 * @param readTimeout    how long to wait for a response once connected
 */
@ConfigurationProperties(prefix = "novamart.internal")
public record InternalClientProperties(
        String token,
        Duration connectTimeout,
        Duration readTimeout) {

    public InternalClientProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
    }
}
