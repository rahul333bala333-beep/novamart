package com.novamart.common.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ownership rule.
 *
 * `canActOnBehalfOf` is called on every read of a cart, an order and a payment,
 * so it is the single check standing between one shopper and another's data.
 */
class AuthenticatedUserTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void aShopperMayActOnTheirOwnDataOnly() {
        var alice = new AuthenticatedUser(ALICE, "alice@example.test", Set.of("USER"));

        assertThat(alice.canActOnBehalfOf(ALICE)).isTrue();
        assertThat(alice.canActOnBehalfOf(BOB)).isFalse();
    }

    @Test
    void anAdministratorMayActOnAnyone() {
        var admin = new AuthenticatedUser(ALICE, "admin@novamart.dev", Set.of("ADMIN"));

        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.canActOnBehalfOf(BOB)).isTrue();
    }

    @Test
    void anInternalServiceMayActOnAnyone() {
        // order-service reads a shopper's cart during checkout, so it has to be
        // able to act for a user it is not.
        var service = AuthenticatedUser.service("order");

        assertThat(service.isService()).isTrue();
        assertThat(service.isAdmin()).isFalse();
        assertThat(service.canActOnBehalfOf(BOB)).isTrue();
    }

    @Test
    void theServicePrincipalIsNotARealUserId() {
        // A fixed all-zero id, so a service principal can never collide with a
        // person and can be spotted immediately in a log.
        assertThat(AuthenticatedUser.service("cart").id())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void aShopperIsNeitherAdminNorService() {
        var user = new AuthenticatedUser(ALICE, "alice@example.test", Set.of("USER"));
        assertThat(user.isAdmin()).isFalse();
        assertThat(user.isService()).isFalse();
    }
}
