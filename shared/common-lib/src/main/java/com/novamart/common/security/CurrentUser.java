package com.novamart.common.security;

import com.novamart.common.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads the verified caller out of the security context.
 *
 * <p>Controllers use this instead of accepting a user id as a parameter. A user
 * id in a path or body is client-controlled and would let anyone read anyone
 * else's cart simply by changing a number; the security context can only have
 * been populated from a checked signature.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user
                ? Optional.of(user)
                : Optional.empty();
    }

    /** The caller, or a 401 if the request is anonymous. */
    public static AuthenticatedUser require() {
        return find().orElseThrow(ApiException::unauthorized);
    }

    public static UUID requireId() {
        return require().id();
    }

    /** Asserts the caller owns the resource, or is an administrator or service. */
    public static void requireOwnershipOf(UUID ownerId) {
        if (!require().canActOnBehalfOf(ownerId)) {
            throw ApiException.forbidden();
        }
    }
}
