-- order-service schema.

CREATE TABLE orders (
    id                      VARCHAR(36)           NOT NULL,
    order_number            VARCHAR(30)    NOT NULL,
    user_id                 VARCHAR(36)           NOT NULL,
    status                  VARCHAR(20)    NOT NULL,
    subtotal                NUMERIC(12, 2) NOT NULL,
    delivery_fee            NUMERIC(12, 2) NOT NULL,
    discount                NUMERIC(12, 2) NOT NULL,
    total                   NUMERIC(12, 2) NOT NULL,
    currency                VARCHAR(3)     NOT NULL,

    -- The delivery address is COPIED onto the order, not referenced by id.
    -- Referencing it would mean a shopper later editing their address book
    -- silently rewrites where a parcel was actually sent months ago.
    ship_label              VARCHAR(40),
    ship_recipient          VARCHAR(120)   NOT NULL,
    ship_phone              VARCHAR(20)    NOT NULL,
    ship_line1              VARCHAR(200)   NOT NULL,
    ship_line2              VARCHAR(200),
    ship_city               VARCHAR(80)    NOT NULL,
    ship_state              VARCHAR(80)    NOT NULL,
    ship_postal_code        VARCHAR(16)    NOT NULL,
    ship_country            VARCHAR(80)    NOT NULL,

    payment_id              VARCHAR(36),
    payment_status          VARCHAR(20)    NOT NULL,
    payment_method          VARCHAR(30)    NOT NULL,
    estimated_delivery_date DATE,
    notes                   VARCHAR(500),
    cancelled_reason        VARCHAR(500),
    idempotency_key         VARCHAR(100),
    placed_at               TIMESTAMP      NOT NULL,
    updated_at              TIMESTAMP      NOT NULL,

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_number UNIQUE (order_number),
    -- Makes checkout safe to retry. A double-clicked pay button or a client
    -- retry after a dropped response hits this constraint instead of creating a
    -- second order and taking a second payment.
    CONSTRAINT uq_orders_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_orders_total_non_negative CHECK (total >= 0)
);

CREATE INDEX idx_orders_user_placed ON orders (user_id, placed_at);
CREATE INDEX idx_orders_status ON orders (status);

-- Line items carry a full snapshot of the product at purchase time: name, SKU,
-- image and unit price. There is no foreign key to a products table because
-- products live in another service's database, and there is no live lookup
-- because a price change next month must not rewrite what was charged last
-- month.
CREATE TABLE order_items (
    id         VARCHAR(36)           NOT NULL,
    order_id   VARCHAR(36)           NOT NULL,
    product_id VARCHAR(36)           NOT NULL,
    sku        VARCHAR(40),
    name       VARCHAR(180)   NOT NULL,
    slug       VARCHAR(220),
    image_url  VARCHAR(500),
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity   INTEGER        NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- Append-only timeline. This is what the order tracking page renders and what
-- support reads when a shopper asks what happened and when.
CREATE TABLE order_events (
    id          VARCHAR(36)        NOT NULL,
    order_id    VARCHAR(36)        NOT NULL,
    status      VARCHAR(20) NOT NULL,
    note        VARCHAR(500),
    occurred_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_order_events PRIMARY KEY (id),
    CONSTRAINT fk_order_events_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_events_order ON order_events (order_id);

-- Allocates the human-facing order number.
--
-- A native SEQUENCE would be the natural choice, but reading one needs
-- dialect-specific SQL (`nextval(...)` on PostgreSQL, `next value for ...` on
-- H2). Every other migration here runs unmodified on both engines, which is what
-- lets the tests and the `local` profile exercise the real schema. A counter row
-- taken under a row lock keeps that property.
CREATE TABLE order_number_counter (
    id         INTEGER NOT NULL,
    next_value BIGINT  NOT NULL,
    CONSTRAINT pk_order_number_counter PRIMARY KEY (id),
    CONSTRAINT ck_order_number_counter_single_row CHECK (id = 1)
);

INSERT INTO order_number_counter (id, next_value) VALUES (1, 1001);
