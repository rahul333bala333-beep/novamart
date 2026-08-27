package com.novamart.order.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * The order lifecycle, with the legal transitions encoded rather than described.
 *
 * <p>Modelling this as a state machine means an illegal move is impossible by
 * construction instead of being prevented by whichever {@code if} statement
 * happens to guard the endpoint. A shipped order cannot quietly become pending,
 * and a cancelled order is terminal no matter which code path is reached.
 */
public enum OrderStatus {

    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final Set<OrderStatus> CANCELLABLE =
            EnumSet.of(PENDING, CONFIRMED, PROCESSING);

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PROCESSING || target == CANCELLED;
            case PROCESSING -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == OUT_FOR_DELIVERY || target == DELIVERED;
            case OUT_FOR_DELIVERY -> target == DELIVERED;
            // Both terminal. Nothing follows them.
            case DELIVERED, CANCELLED -> false;
        };
    }

    public boolean isCancellable() {
        return CANCELLABLE.contains(this);
    }

    /** True once the order counts towards revenue: confirmed and not cancelled. */
    public boolean isRevenueRecognised() {
        return this != PENDING && this != CANCELLED;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
