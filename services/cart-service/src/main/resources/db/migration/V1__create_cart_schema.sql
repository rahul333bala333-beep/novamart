-- cart-service schema.
--
-- Note the absence of a price column on cart_items. A cart stores what a shopper
-- chose, not what it cost at the time they chose it: prices are read live from
-- product-service on every request and frozen exactly once, by order-service,
-- when the order is placed.

CREATE TABLE carts (
    id         VARCHAR(36)      NOT NULL,
    user_id    VARCHAR(36)      NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id),
    -- One cart per shopper, enforced here rather than trusted to application code.
    CONSTRAINT uq_carts_user UNIQUE (user_id)
);

CREATE TABLE cart_items (
    id         VARCHAR(36)      NOT NULL,
    cart_id    VARCHAR(36)      NOT NULL,
    product_id VARCHAR(36)      NOT NULL,
    quantity   INTEGER   NOT NULL,
    added_at   TIMESTAMP NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    -- A product appears at most once per cart; adding it again increments the
    -- existing line instead of creating a duplicate the shopper cannot tell apart.
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

-- There is no foreign key from cart_items.product_id to a products table, and
-- there cannot be: products live in a different service and a different
-- database. Referential integrity across a service boundary is maintained by the
-- API contract and by cart-service dropping lines whose product no longer
-- resolves, not by the database. That is the cost of service autonomy, and it is
-- a deliberate trade rather than an oversight.
