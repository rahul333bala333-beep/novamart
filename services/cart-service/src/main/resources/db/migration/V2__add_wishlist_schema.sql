-- Adds wishlist support to cart_db

CREATE TABLE wishlist_items (
    id         VARCHAR(36)      NOT NULL,
    user_id    VARCHAR(36)      NOT NULL,
    product_id VARCHAR(36)      NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_wishlist_items PRIMARY KEY (id),
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_wishlist_user ON wishlist_items (user_id);
