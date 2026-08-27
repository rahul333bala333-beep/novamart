package com.novamart.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings, bound from {@code novamart.jwt.*}.
 *
 * <p>The secret has no default. A missing or too-short value fails the
 * application at startup rather than silently falling back to a well-known
 * string, which is the failure mode that turns a demo credential into a
 * production vulnerability. The local development profile supplies a clearly
 * labelled throwaway value; every other environment must inject one.
 *
 * @param secret          HMAC signing key, minimum 32 bytes for HS256
 * @param issuer          value placed in, and required from, the {@code iss} claim
 * @param accessTokenTtl  lifetime of an access token; short by design
 * @param refreshTokenTtl lifetime of a refresh token
 */
@ConfigurationProperties(prefix = "novamart.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {

    public JwtProperties {
        issuer = (issuer == null || issuer.isBlank()) ? "novamart" : issuer;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofHours(1) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(14) : refreshTokenTtl;
    }
}
