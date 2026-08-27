package com.novamart.gateway.filter;

import com.novamart.common.security.JwtProperties;
import com.novamart.common.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The most security-critical class in the platform.
 *
 * Downstream services find it convenient to read {@code X-User-Id}. If the
 * gateway merely ADDED that header, a caller could send their own and have it
 * forwarded untouched, and any service trusting it would serve another shopper's
 * cart. {@code X-Internal-Token} is worse still: it grants the SERVICE role,
 * which unlocks the internal API.
 *
 * So the contract these tests pin down is: those headers are removed from every
 * inbound request unconditionally, and only re-added from a verified signature.
 */
class HeaderHygieneFilterTest {

    private static final String SECRET = "gateway-test-signing-key-at-least-32-bytes!!";

    private final JwtService jwt = new JwtService(
            new JwtProperties(SECRET, "novamart", Duration.ofHours(1), Duration.ofDays(14)));

    private final HeaderHygieneFilter filter = new HeaderHygieneFilter(jwt);

    /** Runs the filter and hands back the request as the downstream service would see it. */
    private ServerHttpRequest forward(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerHttpRequest> seen = new AtomicReference<>();
        filter.filter(exchange, downstream -> {
            seen.set(downstream.getRequest());
            return Mono.empty();
        }).block();
        return seen.get();
    }

    // -------------------------------------------------------- stripping --

    @Test
    @DisplayName("a client-supplied X-User-Id is discarded, not forwarded")
    void spoofedUserIdIsStripped() {
        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/cart")
                .header("X-User-Id", "11111111-1111-4111-8111-111111111111")
                .build());

        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    @DisplayName("a client-supplied X-Internal-Token is discarded")
    void spoofedInternalTokenIsStripped() {
        // Forwarding this would hand an outsider the SERVICE role and with it
        // every /internal/** endpoint in the platform.
        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/internal/carts/11111111-1111-4111-8111-111111111111")
                .header("X-Internal-Token", "guessed-or-leaked-token")
                .build());

        assertThat(forwarded.getHeaders().getFirst("X-Internal-Token")).isNull();
    }

    @Test
    void everySpoofableHeaderIsStripped() {
        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/orders")
                .header("X-User-Id", "forged")
                .header("X-User-Email", "admin@novamart.dev")
                .header("X-User-Roles", "ADMIN")
                .header("X-Internal-Token", "forged")
                .build());

        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.getHeaders().getFirst("X-User-Email")).isNull();
        assertThat(forwarded.getHeaders().getFirst("X-User-Roles")).isNull();
        assertThat(forwarded.getHeaders().getFirst("X-Internal-Token")).isNull();
    }

    @Test
    @DisplayName("spoofed headers are stripped even when a valid token is present")
    void aValidTokenDoesNotLetSpoofedHeadersThrough() {
        UUID real = UUID.randomUUID();
        String token = jwt.issueAccessToken(real, "shopper@example.test", Set.of("USER"));

        // The dangerous case: sign in as yourself, then claim to be someone else.
        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/cart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "11111111-1111-4111-8111-111111111111")
                .header("X-User-Roles", "ADMIN")
                .header("X-Internal-Token", "forged")
                .build());

        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isEqualTo(real.toString());
        assertThat(forwarded.getHeaders().getFirst("X-User-Roles")).isEqualTo("USER");
        assertThat(forwarded.getHeaders().getFirst("X-Internal-Token")).isNull();
    }

    // -------------------------------------------------------- injecting --

    @Test
    void averifiedTokenProducesTrustedIdentityHeaders() {
        UUID userId = UUID.randomUUID();
        String token = jwt.issueAccessToken(userId, "demo@novamart.dev", Set.of("USER", "ADMIN"));

        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());

        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isEqualTo(userId.toString());
        assertThat(forwarded.getHeaders().getFirst("X-User-Email")).isEqualTo("demo@novamart.dev");
        assertThat(forwarded.getHeaders().getFirst("X-User-Roles")).contains("USER", "ADMIN");
    }

    @Test
    void theAuthorizationHeaderIsPassedThroughUntouched() {
        // Every service verifies the token itself, so it must still arrive.
        String token = jwt.issueAccessToken(UUID.randomUUID(), "demo@novamart.dev", Set.of("USER"));

        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/cart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());

        assertThat(forwarded.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + token);
    }

    // --------------------------------------------------------- passing --

    @Test
    @DisplayName("an anonymous request passes through unannotated")
    void anonymousRequestsAreNotBlocked() {
        // Browsing the catalogue must work signed out, so the gateway does not
        // reject a request merely for having no token.
        ServerHttpRequest forwarded = forward(MockServerHttpRequest.get("/api/v1/products").build());

        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    @DisplayName("an invalid token is forwarded unannotated so the service reports the precise error")
    void anInvalidTokenIsNotRejectedAtTheGateway() {
        // The owning service distinguishes TOKEN_EXPIRED from INVALID_TOKEN, and
        // the client needs that difference to decide between refreshing and
        // signing in again. Rejecting here would flatten both into one 401.
        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/cart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt")
                .build());

        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer not.a.real.jwt");
    }

    @Test
    void aTokenSignedWithAnotherKeyProducesNoIdentity() {
        JwtService attacker = new JwtService(
                new JwtProperties("a-totally-different-key-also-long-enough!!!!", "novamart",
                        Duration.ofHours(1), Duration.ofDays(1)));
        String forged = attacker.issueAccessToken(UUID.randomUUID(), "mallory@example.test", Set.of("ADMIN"));

        ServerHttpRequest forwarded = forward(MockServerHttpRequest
                .get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)
                .build());

        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.getHeaders().getFirst("X-User-Roles")).isNull();
    }

    @Test
    void theFilterRunsBeforeEverythingElse() {
        // Nothing may observe the raw headers, so this must be first in the chain.
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void aNonBearerAuthorizationSchemeIsIgnored() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/v1/cart")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build());

        AtomicReference<ServerHttpRequest> seen = new AtomicReference<>();
        filter.filter(exchange, d -> {
            seen.set(d.getRequest());
            return Mono.empty();
        }).block();

        assertThat(seen.get().getHeaders().getFirst("X-User-Id")).isNull();
    }
}
