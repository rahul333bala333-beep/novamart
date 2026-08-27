package com.novamart.auth.service;

import com.novamart.auth.domain.RefreshToken;
import com.novamart.auth.domain.User;
import com.novamart.auth.dto.AuthDtos.AuthTokens;
import com.novamart.auth.dto.AuthDtos.LoginRequest;
import com.novamart.auth.dto.AuthDtos.RegisterRequest;
import com.novamart.auth.dto.AuthDtos.UserProfile;
import com.novamart.auth.repository.RefreshTokenRepository;
import com.novamart.auth.repository.UserRepository;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.common.security.AuthenticatedUser;
import com.novamart.common.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Registration, sign-in and token lifecycle.
 *
 * <p>Logging here names the event and the user id, never the email, the password
 * or the token. Authentication logs are exactly where credentials leak into
 * plain-text storage, so the rule is absolute.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationGateway notifications;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       NotificationGateway notifications) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notifications = notifications;
    }

    @Transactional
    public AuthTokens register(RegisterRequest request) {
        String email = User.normaliseEmail(request.email());
        if (users.existsByEmail(email)) {
            // Deliberately the same wording the API contract documents. It does
            // reveal that an account exists, which is the accepted trade-off for
            // a usable sign-up form; the alternative silently swallows a real
            // user error. Sign-in below makes the opposite choice.
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.create(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone(),
                Set.of(AuthenticatedUser.ROLE_USER));
        users.save(user);

        log.info("Registered user {}", user.getId());
        notifications.sendWelcome(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        User user = users.findByEmail(User.normaliseEmail(request.email()))
                .orElse(null);

        // Verify a hash even when the user does not exist. Skipping the work
        // would make a miss measurably faster than a hit and turn response time
        // into an account-enumeration oracle.
        String hash = user != null ? user.getPasswordHash() : DUMMY_HASH;
        boolean matches = passwordEncoder.matches(request.password(), hash);

        if (user == null || !matches) {
            log.info("Failed sign-in attempt");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        log.info("User {} signed in", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthTokens refresh(String presentedToken) {
        JwtService.RefreshTokenClaims claims = jwtService.verifyRefreshToken(presentedToken);

        RefreshToken stored = refreshTokens.findById(java.util.UUID.fromString(claims.tokenId()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));

        if (!stored.isUsable()) {
            // A revoked token being presented again is a replay. Revoking the
            // whole family limits what a stolen token is worth.
            log.warn("Replayed refresh token for user {}; revoking all sessions", stored.getUserId());
            refreshTokens.revokeAllForUser(stored.getUserId());
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        User user = users.findById(claims.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        stored.revoke();
        return issueTokens(user);
    }

    @Transactional
    public void logout(String presentedToken) {
        try {
            JwtService.RefreshTokenClaims claims = jwtService.verifyRefreshToken(presentedToken);
            refreshTokens.findById(java.util.UUID.fromString(claims.tokenId()))
                    .ifPresent(RefreshToken::revoke);
        } catch (ApiException ex) {
            // Signing out with an already-invalid token is not an error worth
            // surfacing: the caller wanted the session gone and it is gone.
            log.debug("Sign-out presented an unusable refresh token");
        }
    }

    private AuthTokens issueTokens(User user) {
        RefreshToken record = RefreshToken.issue(user.getId(),
                Instant.now().plus(java.time.Duration.ofDays(14)));
        refreshTokens.save(record);

        String access = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refresh = jwtService.issueRefreshToken(user.getId(), record.getId().toString());

        return new AuthTokens(access, refresh, "Bearer",
                jwtService.accessTokenTtlSeconds(), UserProfile.from(user));
    }

    /**
     * A valid BCrypt digest of a value nobody knows, used only to keep the
     * timing of a failed lookup indistinguishable from a wrong password.
     */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
}
