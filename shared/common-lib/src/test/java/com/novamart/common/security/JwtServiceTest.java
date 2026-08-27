package com.novamart.common.security;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Token issuing and verification.
 *
 * This is the security primitive the entire platform rests on: every service
 * trusts whatever this class says a caller is. The negative cases matter far
 * more than the positive one.
 */
class JwtServiceTest {

    private static final String SECRET = "a-test-signing-key-that-is-definitely-long-enough-32";

    private static JwtProperties props(String secret) {
        return new JwtProperties(secret, "novamart", Duration.ofHours(1), Duration.ofDays(14));
    }

    private final JwtService jwt = new JwtService(props(SECRET));

    // ------------------------------------------------------ construction --

    @Test
    @DisplayName("a missing secret fails fast rather than defaulting")
    void aBlankSecretIsRejectedAtConstruction() {
        // Failing at startup is the whole point: a service that silently fell
        // back to a default key would sign tokens anyone could forge.
        assertThatThrownBy(() -> new JwtService(props(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOVAMART_JWT_SECRET");

        assertThatThrownBy(() -> new JwtService(props("   ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aShortSecretIsRejected() {
        // HS256 requires 256 bits. A shorter key weakens every token issued.
        assertThatThrownBy(() -> new JwtService(props("too-short")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    // ---------------------------------------------------------- issuing --

    @Test
    void anAccessTokenRoundTripsIdentityAndRoles() {
        UUID userId = UUID.randomUUID();
        String token = jwt.issueAccessToken(userId, "demo@novamart.dev", Set.of("USER", "ADMIN"));

        AuthenticatedUser user = jwt.verifyAccessToken(token);
        assertThat(user.id()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo("demo@novamart.dev");
        assertThat(user.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void aRefreshTokenCarriesItsIdSoItCanBeRevoked() {
        UUID userId = UUID.randomUUID();
        String tokenId = UUID.randomUUID().toString();

        var claims = jwt.verifyRefreshToken(jwt.issueRefreshToken(userId, tokenId));
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.tokenId()).isEqualTo(tokenId);
    }

    // ------------------------------------------------------- rejection --

    @Test
    @DisplayName("a token signed with a different key is rejected")
    void aForgedSignatureIsRejected() {
        JwtService attacker = new JwtService(props("a-completely-different-key-also-long-enough!!"));
        String forged = attacker.issueAccessToken(UUID.randomUUID(), "mallory@example.test", Set.of("ADMIN"));

        assertThatThrownBy(() -> jwt.verifyAccessToken(forged))
                .isInstanceOf(ApiException.class)
                .satisfies(t -> assertThat(((ApiException) t).errorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    void aTamperedPayloadIsRejected() {
        String token = jwt.issueAccessToken(UUID.randomUUID(), "demo@novamart.dev", Set.of("USER"));
        // Flip a character in the payload segment; the signature no longer matches.
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[5] = payload[5] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThatThrownBy(() -> jwt.verifyAccessToken(tampered)).isInstanceOf(ApiException.class);
    }

    @Test
    void garbageIsRejectedRatherThanThrowingSomethingUnexpected() {
        // Whatever arrives in the Authorization header, the caller must get a
        // clean 401 rather than a 500 from an unhandled parse error.
        for (String rubbish : new String[] {"", "not-a-jwt", "a.b.c", "...", "null"}) {
            assertThatThrownBy(() -> jwt.verifyAccessToken(rubbish))
                    .as("input %s", rubbish)
                    .isInstanceOf(ApiException.class);
        }
    }

    @Test
    @DisplayName("an expired token reports TOKEN_EXPIRED, not INVALID_TOKEN")
    void expiryIsDistinguishableFromInvalidity() {
        JwtService shortLived = new JwtService(
                new JwtProperties(SECRET, "novamart", Duration.ofSeconds(-1), Duration.ofDays(1)));
        String expired = shortLived.issueAccessToken(UUID.randomUUID(), "demo@novamart.dev", Set.of("USER"));

        // The client needs to tell "refresh silently" apart from "sign in again".
        assertThatThrownBy(() -> shortLived.verifyAccessToken(expired))
                .isInstanceOf(ApiException.class)
                .satisfies(t -> assertThat(((ApiException) t).errorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void aRefreshTokenCannotAuthoriseARequest() {
        String refresh = jwt.issueRefreshToken(UUID.randomUUID(), UUID.randomUUID().toString());

        // Same key signs both, so only the `typ` claim separates them.
        assertThatThrownBy(() -> jwt.verifyAccessToken(refresh))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("refresh token cannot be used");
    }

    @Test
    void anAccessTokenCannotBeUsedToRefresh() {
        String access = jwt.issueAccessToken(UUID.randomUUID(), "demo@novamart.dev", Set.of("USER"));

        assertThatThrownBy(() -> jwt.verifyRefreshToken(access))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("access token cannot be used");
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() {
        JwtService other = new JwtService(new JwtProperties(SECRET, "someone-else",
                Duration.ofHours(1), Duration.ofDays(1)));
        String token = other.issueAccessToken(UUID.randomUUID(), "demo@novamart.dev", Set.of("USER"));

        assertThatThrownBy(() -> jwt.verifyAccessToken(token)).isInstanceOf(ApiException.class);
    }
}
