package com.novamart.order.domain;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static Order newOrder() {
        return Order.open("NM-2026-0000001", UUID.randomUUID(),
                new ShippingAddress("Home", "Ananya Iyer", "+91 98450 22002",
                        "14 Brigade Gardens", null, "Bengaluru", "Karnataka", "560034", "India"),
                "MOCK_CARD", null, null);
    }

    @Test
    void aNewOrderStartsPendingWithAnOpeningTimelineEntry() {
        Order order = newOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTimeline()).hasSize(1);
        assertThat(order.getTimeline().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void everyTransitionAppendsToTheTimeline() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED, "Payment success");
        order.transitionTo(OrderStatus.PROCESSING, null);

        assertThat(order.getTimeline())
                .extracting(OrderEvent::getStatus)
                .containsExactly(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);
    }

    @Test
    void anIllegalTransitionIsRefusedWithAConflict() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DELIVERED, null))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> assertThat(((ApiException) thrown).errorCode())
                        .isEqualTo(ErrorCode.INVALID_ORDER_TRANSITION));

        // The order must be untouched after a rejected transition.
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTimeline()).hasSize(1);
    }

    @Test
    void totalsAreSubtotalPlusDeliveryMinusDiscount() {
        Order order = newOrder();
        order.applyTotals(new BigDecimal("12998.00"), new BigDecimal("79.00"),
                new BigDecimal("100.00"), "INR");

        assertThat(order.getTotal()).isEqualByComparingTo("12977.00");
    }

    @Test
    void totalsUseExactDecimalArithmetic() {
        Order order = newOrder();
        // 0.1 + 0.2 is 0.30000000000000004 in binary floating point. This is the
        // whole reason money is BigDecimal throughout.
        order.applyTotals(new BigDecimal("0.10"), new BigDecimal("0.20"), BigDecimal.ZERO, "INR");
        assertThat(order.getTotal()).isEqualByComparingTo("0.30");
    }

    @Test
    void cancellingRecordsTheReasonAndEndsTheOrder() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED, null);
        order.cancel("Changed my mind");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledReason()).isEqualTo("Changed my mind");
    }

    @Test
    void aDeliveredOrderCannotBeCancelled() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED, null);
        order.transitionTo(OrderStatus.PROCESSING, null);
        order.transitionTo(OrderStatus.SHIPPED, null);
        order.transitionTo(OrderStatus.DELIVERED, null);

        assertThatThrownBy(() -> order.cancel("too late"))
                .isInstanceOf(ApiException.class);
    }
}
