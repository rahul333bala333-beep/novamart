-- Adds product_reviews table to product_db

CREATE TABLE product_reviews (
    id                VARCHAR(36)          NOT NULL,
    product_id        VARCHAR(36)          NOT NULL,
    user_id           VARCHAR(36)          NOT NULL,
    user_name         VARCHAR(120)  NOT NULL,
    rating            INTEGER       NOT NULL,
    title             VARCHAR(180)  NOT NULL,
    comment           VARCHAR(3000) NOT NULL,
    verified_purchase BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    CONSTRAINT pk_product_reviews PRIMARY KEY (id),
    CONSTRAINT uq_reviews_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_reviews_rating_range CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_reviews_product ON product_reviews (product_id);
CREATE INDEX idx_reviews_user ON product_reviews (user_id);
