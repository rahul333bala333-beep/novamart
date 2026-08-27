package com.novamart.cart.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cart mutation rules.
 *
 * Small surface, but the increment-versus-duplicate behaviour is the one a
 * shopper notices immediately when it is wrong.
 */
class CartTest {

    private static final UUID PRODUCT = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    void aNewCartIsEmptyAndBelongsToItsUser() {
        UUID userId = UUID.randomUUID();
        Cart cart = Cart.forUser(userId);

        assertThat(cart.getUserId()).isEqualTo(userId);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("adding a product already present increments rather than duplicating")
    void addingTwiceIncrementsTheExistingLine() {
        Cart cart = Cart.forUser(UUID.randomUUID());
        cart.addOrIncrement(PRODUCT, 1);
        cart.addOrIncrement(PRODUCT, 2);

        // Two lines for the same product look like a bug to a shopper and make
        // the quantity stepper ambiguous.
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.findItem(PRODUCT).orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test
    void differentProductsGetTheirOwnLines() {
        Cart cart = Cart.forUser(UUID.randomUUID());
        cart.addOrIncrement(PRODUCT, 1);
        cart.addOrIncrement(OTHER, 1);

        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    void settingAQuantityReplacesRatherThanAdds() {
        Cart cart = Cart.forUser(UUID.randomUUID());
        cart.addOrIncrement(PRODUCT, 5);
        cart.setQuantity(PRODUCT, 2);

        assertThat(cart.findItem(PRODUCT).orElseThrow().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("setting a quantity of zero removes the line")
    void zeroQuantityRemovesTheLine() {
        Cart cart = Cart.forUser(UUID.randomUUID());
        cart.addOrIncrement(PRODUCT, 3);
        cart.setQuantity(PRODUCT, 0);

        // A line with quantity zero would render as "0 x product" and violate the
        // database check constraint on the way out.
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removingReportsWhetherAnythingWasThere() {
        Cart cart = Cart.forUser(UUID.randomUUID());
        cart.addOrIncrement(PRODUCT, 1);

        assertThat(cart.remove(PRODUCT)).isTrue();
        // The second call tells the service to answer CART_ITEM_NOT_FOUND
        // rather than silently succeeding.
        assertThat(cart.remove(PRODUCT)).isFalse();
    }

    @Test
    void clearingEmptiesEverything() {
        Cart cart = Cart.forUser(UUID.randomUUID());
        cart.addOrIncrement(PRODUCT, 1);
        cart.addOrIncrement(OTHER, 2);
        cart.clear();

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void everyMutationBumpsTheTimestamp() throws Exception {
        Cart cart = Cart.forUser(UUID.randomUUID());
        var before = cart.getUpdatedAt();
        Thread.sleep(5);
        cart.addOrIncrement(PRODUCT, 1);

        assertThat(cart.getUpdatedAt()).isAfter(before);
    }
}
