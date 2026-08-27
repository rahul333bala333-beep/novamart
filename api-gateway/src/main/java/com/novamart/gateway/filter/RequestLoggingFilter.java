package com.novamart.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * One log line per request, with a correlation id.
 *
 * <p>The gateway is the only place that sees every request, which makes it the
 * right place to stamp a correlation id. Without one, tracing a single checkout
 * across six services means guessing from timestamps.
 *
 * <p>What is logged is method, path, status, duration and the route that
 * matched. Query strings are deliberately excluded: they carry search terms and
 * filters, and logging them at the edge is how personal data quietly ends up in
 * log aggregation. Authorization headers and request bodies are never touched.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String id = correlationId;

        var mutated = exchange.mutate()
                .request(exchange.getRequest().mutate().header(CORRELATION_ID, id).build())
                .build();
        mutated.getResponse().getHeaders().set(CORRELATION_ID, id);

        long start = System.nanoTime();
        String method = String.valueOf(exchange.getRequest().getMethod());
        String path = exchange.getRequest().getPath().value();

        return chain.filter(mutated).doFinally(signal -> {
            long millis = (System.nanoTime() - start) / 1_000_000;
            Object route = mutated.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            var status = mutated.getResponse().getStatusCode();
            log.info("{} {} -> {} [{}] {}ms cid={}",
                    method, path,
                    status == null ? "-" : status.value(),
                    route instanceof org.springframework.cloud.gateway.route.Route r ? r.getId() : "unrouted",
                    millis, id);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
