-- inventory-service schema.

CREATE TABLE inventory_items (
    product_id        VARCHAR(36)      NOT NULL,
    total_quantity    INTEGER   NOT NULL,
    reserved_quantity INTEGER   NOT NULL DEFAULT 0,
    reorder_threshold INTEGER   NOT NULL DEFAULT 5,
    version           BIGINT    NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP NOT NULL,
    CONSTRAINT pk_inventory_items PRIMARY KEY (product_id),

    -- These two constraints are the last line of defence for the invariant the
    -- whole service exists to protect. The application already refuses to
    -- oversell, but a bug, a manual UPDATE or a future code path could still try;
    -- the database refuses regardless of who is asking.
    CONSTRAINT ck_inventory_non_negative CHECK (total_quantity >= 0 AND reserved_quantity >= 0),
    CONSTRAINT ck_inventory_reserved_within_total CHECK (reserved_quantity <= total_quantity)
);

-- Append-only ledger. Every movement is recorded in the same transaction as the
-- mutation it describes, so the running total and its history cannot disagree.
CREATE TABLE stock_transactions (
    id                 VARCHAR(36)        NOT NULL,
    product_id         VARCHAR(36)        NOT NULL,
    type               VARCHAR(30) NOT NULL,
    quantity           INTEGER     NOT NULL,
    resulting_total    INTEGER     NOT NULL,
    resulting_reserved INTEGER     NOT NULL,
    reference_id       VARCHAR(80),
    occurred_at        TIMESTAMP   NOT NULL,
    CONSTRAINT pk_stock_transactions PRIMARY KEY (id)
);

CREATE INDEX idx_stock_transactions_product ON stock_transactions (product_id);
CREATE INDEX idx_stock_transactions_reference ON stock_transactions (reference_id);
