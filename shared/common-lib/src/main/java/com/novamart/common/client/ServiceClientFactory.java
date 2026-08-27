package com.novamart.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novamart.common.api.ErrorResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Builds the {@link RestClient} instances services use to call each other.
 *
 * <p>Three things are centralised here rather than repeated per call site:
 *
 * <ol>
 *   <li><b>Timeouts.</b> An unbounded read timeout is how one slow service takes
 *       down every service that depends on it. Both timeouts are always set.</li>
 *   <li><b>Credentials.</b> Internal endpoints are not public, so every outbound
 *       call carries the shared service token.</li>
 *   <li><b>Error translation.</b> A downstream failure is decoded from the
 *       standard envelope and rethrown with its original {@link ErrorCode}, so
 *       {@code INSUFFICIENT_STOCK} raised by inventory-service still reaches the
 *       shopper as a 409 with that code instead of being flattened to a 500.</li>
 * </ol>
 */
public class ServiceClientFactory {

    private static final Logger log = LoggerFactory.getLogger(ServiceClientFactory.class);

    private final InternalClientProperties properties;
    private final ObjectMapper objectMapper;

    public ServiceClientFactory(InternalClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public RestClient create(String baseUrl, String downstreamName) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .defaultHeader(com.novamart.common.security.JwtAuthenticationFilter.INTERNAL_TOKEN_HEADER,
                        properties.token() == null ? "" : properties.token())
                .defaultStatusHandler(
                        status -> status.isError(),
                        (request, response) -> {
                            throw translate(response.getStatusCode().value(), response.getBody(), downstreamName);
                        })
                .build();
    }

    /**
     * Converts a downstream error body into an {@link ApiException}. When the
     * body is not a recognisable envelope the failure is reported as
     * {@code SERVICE_UNAVAILABLE} rather than guessed at.
     */
    private ApiException translate(int status, InputStream body, String downstreamName) {
        try {
            byte[] raw = body.readAllBytes();
            ErrorResponse error = objectMapper.readValue(raw, ErrorResponse.class);
            if (error.errorCode() != null) {
                ErrorCode code = ErrorCode.valueOf(error.errorCode());
                log.debug("{} responded {} {}", downstreamName, status, code);
                return new ApiException(code, error.message());
            }
            log.warn("{} responded {} with an envelope carrying no error code: {}",
                    downstreamName, status, new String(raw, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            log.warn("{} responded {} with an unrecognised error code", downstreamName, status);
        } catch (IOException | RuntimeException ex) {
            log.warn("{} responded {} with an unreadable body", downstreamName, status);
        }
        return new ApiException(ErrorCode.SERVICE_UNAVAILABLE,
                "The " + downstreamName + " service could not complete the request");
    }
}
