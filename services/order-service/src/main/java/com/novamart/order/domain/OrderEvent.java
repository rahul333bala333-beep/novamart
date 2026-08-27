package com.novamart.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One entry in an order's timeline, appended on every status change.
 *
 * <p>This is what turns "your order is shipped" into "placed Tuesday, confirmed
 * Tuesday, shipped Thursday" on the order page, and it is the record support
 * needs when a shopper asks what happened and when.
 */
@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_events_order"))
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected OrderEvent() {
    }

    static OrderEvent record(Order order, OrderStatus status, String note) {
        OrderEvent event = new OrderEvent();
        event.id = UUID.randomUUID();
        event.order = order;
        event.status = status;
        event.note = note;
        event.occurredAt = Instant.now();
        return event;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
