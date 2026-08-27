package com.novamart.gateway.filter;

import com.novamart.common.security.AuthenticatedUser;
import com.novamart.common.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Scrubs inbound identity headers and replaces them with trusted ones.
 *
 * <p>This is the most security-critical class in the gateway, and it exists
 * because of one attack. Downstream services find it convenient to read
 * {@code X-User-Id}. If the gateway merely <em>adds</em> that header, then a
 * caller who sets it themselves has it forwarded untouched, and any service that
 * trusts it will happily serve another shopper's cart. Worse,
 * {@code X-Internal-Token} is the credential that grants the SERVICE role, so a
 * forwarded copy of it would hand an outsider the internal API.
 *
 * <p>So every one of these headers is <b>removed unconditionally</b> from every
 * inbound request first, and only then re-added from a verified token. A client
 * cannot inject them, because whatever they send is discarded before anything
 * looks at it.
 *
 * <p>Defence in depth: services do not actually rely on these headers for
 * authorisation. Each one independently verifies the JWT itself, so a service
 * reachable directly is still safe. The headers exist for logging and
 * convenience, and are made trustworthy here regardless.
 */
@Component
public class HeaderHygieneFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(HeaderHygieneFilter.class);

    private static final String USER_ID = "X-User-Id";
    private static final String USER_EMAIL = "X-User-Email";
    private static final String USER_ROLES = "X-User-Roles";
    private static final String INTERNAL_TOKEN = "X-Internal-Token";
    private static final List<String> STRIPPED =
            List.of(USER_ID, USER_EMAIL, USER_ROLES, INTERNAL_TOKEN);

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public HeaderHygieneFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest incoming = exchange.getRequest();

        for (String header : STRIPPED) {
            if (incoming.getHeaders().containsKey(header)) {
                log.warn("Rejected client-supplied {} header on {} {}; stripping before routing",
                        header, incoming.getMethod(), incoming.getPath());
            }
        }

        ServerHttpRequest.Builder mutated = incoming.mutate()
                .headers(headers -> STRIPPED.forEach(headers::remove));

        // Add trusted identity only when a signature actually verifies. An invalid
        // or expired token is passed through unannotated rather than rejected
        // here, so the owning service produces the precise error code (expired vs
        // malformed) that the client needs to decide between refreshing and
        // signing in again.
        String authorization = incoming.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER)) {
            try {
                AuthenticatedUser user = jwtService.verifyAccessToken(
                        authorization.substring(BEARER.length()).trim());
                mutated.header(USER_ID, user.id().toString())
                        .header(USER_EMAIL, user.email() == null ? "" : user.email())
                        .header(USER_ROLES, String.join(",", user.roles()));
            } catch (RuntimeException ex) {
                log.debug("Token on {} {} did not verify at the gateway: {}",
                        incoming.getMethod(), incoming.getPath(), ex.getMessage());
            }
        }

        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    @Override
    public int getOrder() {
        // Before every other filter. Nothing may observe the raw headers.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
