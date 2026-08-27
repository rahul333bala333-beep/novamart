package com.novamart.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Renders gateway-level failures in the same envelope the services use.
 *
 * <p>Without this, a service being down produces Spring's default reactive error
 * body, which has a different shape from every successful response. A client
 * would then need two parsers, and the one it needs least often is the one it
 * would reach for during an outage.
 *
 * <p>An unreachable or slow service is reported as {@code SERVICE_UNAVAILABLE}
 * with a 503, which tells a client the request may succeed on retry. Returning
 * 500 would suggest the request itself was at fault.
 */
@Component
@Order(-1)
public class GatewayErrorHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ErrorAttributes errorAttributes,
                               ApplicationContext applicationContext,
                               ServerCodecConfigurer codecConfigurer,
                               ObjectMapper objectMapper) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        this.setMessageWriters(codecConfigurer.getWriters());
        this.setMessageReaders(codecConfigurer.getReaders());
        this.objectMapper = objectMapper;
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::render);
    }

    private Mono<ServerResponse> render(ServerRequest request) {
        Throwable error = getError(request);
        String path = request.path();

        HttpStatus status;
        String errorCode;
        String message;

        if (error instanceof ConnectException || error instanceof TimeoutException
                || (error.getCause() instanceof ConnectException)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "SERVICE_UNAVAILABLE";
            message = "That part of the service is temporarily unavailable. Please try again shortly.";
            log.error("Downstream service unreachable for {}: {}", path, error.toString());
        } else if (error instanceof org.springframework.web.server.ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            errorCode = status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : "GATEWAY_ERROR";
            message = status == HttpStatus.NOT_FOUND
                    ? "No route matches that address."
                    : "The request could not be routed.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_ERROR";
            message = "Something went wrong on our side. Please try again.";
            // Full detail stays server-side; the client gets the generic message.
            log.error("Unhandled gateway error for {}", path, error);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("errorCode", errorCode);
        body.put("timestamp", Instant.now().toString());
        body.put("path", path);

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body((outbound, context) -> {
                    try {
                        byte[] bytes = objectMapper.writeValueAsBytes(body);
                        return outbound.writeWith(Mono.just(
                                new DefaultDataBufferFactory().wrap(bytes)));
                    } catch (Exception ex) {
                        return Mono.error(ex);
                    }
                });
    }

    @SuppressWarnings("unused")
    private Map<String, Object> attributes(ServerRequest request) {
        return getErrorAttributes(request, ErrorAttributeOptions.defaults());
    }
}
