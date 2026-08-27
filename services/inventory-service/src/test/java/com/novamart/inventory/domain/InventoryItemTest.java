package com.novamart.inventory.domain;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The reserve / release / commit protocol.
 *
 * These are the invariants that stop the shop overselling, so they are tested
 * directly on the aggregate where the arithmetic lives, rather than only through
 * an HTTP endpoint several layers away.
 */
class InventoryItemTest {

    private static InventoryItem stocked(int quantity) {
        return InventoryItem.create(UUID.randomUUID(), quantity, 5);
    }

    @Test
    void availabilityIsTotalMinusReserved() {
        InventoryItem item = stocked(10);
        item.reserve(3);

        assertThat(item.getTotalQuantity()).isEqualTo(10);
        assertThat(item.getReservedQuantity()).isEqualTo(3);
        assertThat(item.availableQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("reserving does not remove stock, only spoken-for stock")
    void reservingHoldsWithoutConsuming() {
        InventoryItem item = stocked(10);
        item.reserve(4);
        // The units are still physically present; they are simply not sellable.
        assertThat(item.getTotalQuantity()).isEqualTo(10);
    }

    @Test
    void cannotReserveMoreThanIsAvailable() {
        InventoryItem item = stocked(2);

        assertThatThrownBy(() -> item.reserve(3))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> assertThat(((ApiException) thrown).errorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_STOCK))
                .hasMessageContaining("2 available");

        assertThat(item.getReservedQuantity()).isZero();
    }

    @Test
    void reservedStockIsNotAvailableToASecondReservation() {
        InventoryItem item = stocked(5);
        item.reserve(5);

        // This is the oversell that the two-counter model exists to prevent.
        assertThatThrownBy(() -> item.reserve(1)).isInstanceOf(ApiException.class);
    }

    @Test
    void committingConsumesStockPermanently() {
        InventoryItem item = stocked(10);
        item.reserve(3);
        item.commit(3);

        assertThat(item.getTotalQuantity()).isEqualTo(7);
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(item.availableQuantity()).isEqualTo(7);
    }

    @Test
    void releasingReturnsStockToTheAvailablePool() {
        InventoryItem item = stocked(10);
        item.reserve(4);
        item.release(4);

        assertThat(item.getTotalQuantity()).isEqualTo(10);
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(item.availableQuantity()).isEqualTo(10);
    }

    @Test
    void cannotReleaseMoreThanIsReserved() {
        InventoryItem item = stocked(10);
        item.reserve(2);

        assertThatThrownBy(() -> item.release(3))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> assertThat(((ApiException) thrown).errorCode())
                        .isEqualTo(ErrorCode.INVALID_STOCK_OPERATION));
    }

    @Test
    void cannotCommitMoreThanIsReserved() {
        InventoryItem item = stocked(10);
        item.reserve(2);
        assertThatThrownBy(() -> item.commit(5)).isInstanceOf(ApiException.class);
    }

    @Test
    void aStockTakeCannotDropBelowWhatIsAlreadyReserved() {
        InventoryItem item = stocked(10);
        item.reserve(6);

        // Allowing this would produce a negative availability, which every
        // downstream check would then misread as "plenty in stock".
        assertThatThrownBy(() -> item.adjustTotal(3, 5))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("6 units are reserved");
    }

    @Test
    void quantitiesMustBePositive() {
        InventoryItem item = stocked(10);
        assertThatThrownBy(() -> item.reserve(0)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> item.reserve(-1)).isInstanceOf(ApiException.class);
    }

    @Test
    void lowStockIsFlaggedAtOrBelowTheThreshold() {
        InventoryItem item = InventoryItem.create(UUID.randomUUID(), 6, 5);
        assertThat(item.isLowStock()).isFalse();

        item.reserve(1);   // available now 5, equal to the threshold
        assertThat(item.isLowStock()).isTrue();
    }

    @Test
    void zeroAvailabilityIsOutOfStockEvenWithUnitsOnTheShelf() {
        InventoryItem item = stocked(3);
        item.reserve(3);
        assertThat(item.isInStock()).isFalse();
    }
}
