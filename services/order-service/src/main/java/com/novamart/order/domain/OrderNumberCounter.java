package com.novamart.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single-row counter that allocates human-facing order numbers.
 *
 * <p>A native database SEQUENCE would be the obvious choice, but reading one
 * requires dialect-specific SQL: PostgreSQL spells it {@code nextval('seq')} and
 * H2 spells it {@code next value for seq}. Every other migration in this project
 * runs unmodified on both engines, and breaking that rule here would mean the
 * schema under test stops being the schema that ships.
 *
 * <p>The cost is that allocating a number takes a row lock, so concurrent
 * checkouts serialise for the microseconds it is held. At this scale that is
 * free. At a scale where it is not, the fix is a per-dialect native query behind
 * this same class, and nothing outside it would change.
 */
@Entity
@Table(name = "order_number_counter")
public class OrderNumberCounter {

    /** Always 1. The table holds exactly one row, enforced by a check constraint. */
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    protected OrderNumberCounter() {
    }

    public long take() {
        return nextValue++;
    }
}
