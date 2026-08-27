package com.novamart.common.security;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Issues and verifies the platform's JSON Web Tokens.
 *
 * <p>Both halves live in one class on purpose. auth-service is the only caller
 * that issues; every other service only verifies. Sharing the implementation
 * guarantees the claim names and the signing algorithm cannot drift apart
 * between the issuer and its verifiers.
 *
 * <p>Access and refresh tokens are distinguished by a {@code typ} claim and are
 * checked on use, so a refresh token cannot be replayed as an access token to
 * reach a protected endpoint.
 */
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /** HS256 requires a key of at least 256 bits; anything shorter is rejected outright. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "novamart.jwt.secret is not set. Provide NOVAMART_JWT_SECRET in the environment. "
                            + "The application will not start with an unsigned or default key.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "novamart.jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " bytes for HS256; got " + bytes.length + ".");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String issueAccessToken(UUID userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    /**
     * Refresh tokens carry no roles or email. If a user is demoted, the roles are
     * re-read from the database at refresh time rather than trusted from the token.
     */
    public String issueRefreshToken(UUID userId, String tokenId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(tokenId)
                .subject(userId.toString())
                .issuer(properties.issuer())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.refreshTokenTtl())))
                .signWith(key)
                .compact();
    }

    /** Verifies an access token and returns the identity it asserts. */
    public AuthenticatedUser verifyAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, "A refresh token cannot be used to authorise a request");
        }
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                readRoles(claims));
    }

    /** Verifies a refresh token and returns its {@code jti}, used to look up and revoke it. */
    public RefreshTokenClaims verifyRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, "An access token cannot be used to refresh a session");
        }
        return new RefreshTokenClaims(UUID.fromString(claims.getSubject()), claims.getId());
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            // Distinguished from a bad signature so the client knows to refresh
            // rather than to send the user back to the sign-in screen.
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> readRoles(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        if (raw instanceof Collection<?> collection) {
            Set<String> roles = new LinkedHashSet<>();
            for (Object element : collection) {
                roles.add(String.valueOf(element));
            }
            return roles;
        }
        return Set.of(AuthenticatedUser.ROLE_USER);
    }

    public record RefreshTokenClaims(UUID userId, String tokenId) {
    }
}
