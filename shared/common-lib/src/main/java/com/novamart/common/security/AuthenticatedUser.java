package com.novamart.common.security;

import java.util.Set;
import java.util.UUID;

/**
 * The verified identity behind the current request.
 *
 * <p>Built only from a signature-checked token (or a verified internal service
 * credential). Nothing here is ever taken from a request header a client could
 * set, which is what stops a caller from claiming to be someone else.
 *
 * @param id    the user id, or {@link #SERVICE_PRINCIPAL_ID} for internal calls
 * @param email the user email, or the calling service name
 * @param roles granted roles, without the {@code ROLE_} prefix
 */
public record AuthenticatedUser(UUID id, String email, Set<String> roles) {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** Role granted to trusted service-to-service calls. Never issued to a person. */
    public static final String ROLE_SERVICE = "SERVICE";

    /** Fixed id used when the caller is another service rather than a person. */
    public static final UUID SERVICE_PRINCIPAL_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    public boolean isAdmin() {
        return roles.contains(ROLE_ADMIN);
    }

    public boolean isService() {
        return roles.contains(ROLE_SERVICE);
    }

    /**
     * Whether this principal may act on data belonging to {@code ownerId}.
     * Administrators and internal services may; a shopper may only act on their own.
     */
    public boolean canActOnBehalfOf(UUID ownerId) {
        return isAdmin() || isService() || id.equals(ownerId);
    }

    public static AuthenticatedUser service(String serviceName) {
        return new AuthenticatedUser(SERVICE_PRINCIPAL_ID, serviceName, Set.of(ROLE_SERVICE));
    }
}
