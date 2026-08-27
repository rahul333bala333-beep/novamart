package com.novamart.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order state machine.
 *
 * Worth testing exhaustively because it is the rule that stops an order going
 * backwards, and because every path through it is cheap to exercise: no Spring
 * context, no database, no clock.
 */
class OrderStatusTest {

    @Nested
    @DisplayName("legal transitions")
    class Legal {

        @Test
        void pendingMovesToConfirmedOrCancelled() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        void fulfilmentAdvancesOneStepAtATime() {
            assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
            assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
            assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
        }
    }

    @Nested
    @DisplayName("illegal transitions")
    class Illegal {

        @Test
        void anOrderCannotSkipFulfilmentSteps() {
            // Marking an order delivered straight from confirmed would leave a
            // parcel that was never picked or shipped looking as though it had
            // arrived.
            assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        }

        @Test
        void anOrderCannotGoBackwards() {
            assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PROCESSING)).isFalse();
            assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
            assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING)).isFalse();
        }

        @Test
        void aShippedOrderCannotBeCancelled() {
            // Once a parcel is with the courier, cancelling is a returns problem
            // rather than a state change.
            assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
            assertThat(OrderStatus.SHIPPED.isCancellable()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(OrderStatus.class)
        void terminalStatesAcceptNothing(OrderStatus target) {
            assertThat(OrderStatus.DELIVERED.canTransitionTo(target)).isFalse();
            assertThat(OrderStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(OrderStatus.class)
        void noStatusCanTransitionToItself(OrderStatus status) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("revenue recognition")
    class Revenue {

        @Test
        void unpaidAndCancelledOrdersAreExcluded() {
            // These two exclusions are why the dashboard's revenue figure is not
            // simply the sum of every order.
            assertThat(OrderStatus.PENDING.isRevenueRecognised()).isFalse();
            assertThat(OrderStatus.CANCELLED.isRevenueRecognised()).isFalse();
        }

        @Test
        void everythingFromConfirmedOnwardsCounts() {
            assertThat(OrderStatus.CONFIRMED.isRevenueRecognised()).isTrue();
            assertThat(OrderStatus.PROCESSING.isRevenueRecognised()).isTrue();
            assertThat(OrderStatus.SHIPPED.isRevenueRecognised()).isTrue();
            assertThat(OrderStatus.DELIVERED.isRevenueRecognised()).isTrue();
        }
    }
}
