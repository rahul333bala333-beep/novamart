-- product-service schema.
--
-- Written in SQL that both PostgreSQL and H2 accept, so the same migration
-- history runs in Docker, in the `local` profile and in the tests.

CREATE TABLE categories (
    id          VARCHAR(36)         NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url   VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

CREATE TABLE brands (
    id         VARCHAR(36)         NOT NULL,
    name       VARCHAR(80)  NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uq_brands_slug UNIQUE (slug)
);

-- Money is NUMERIC(12,2), never a floating-point type. Binary floating point
-- cannot represent 0.01 exactly, so accumulating line totals in a double drifts
-- and eventually yields an order total that does not equal the sum of its lines.
CREATE TABLE products (
    id                VARCHAR(36)           NOT NULL,
    sku               VARCHAR(40)    NOT NULL,
    slug              VARCHAR(220)   NOT NULL,
    name              VARCHAR(180)   NOT NULL,
    short_description VARCHAR(300),
    description       VARCHAR(5000)  NOT NULL,
    price             NUMERIC(12, 2) NOT NULL,
    compare_at_price  NUMERIC(12, 2),
    currency          VARCHAR(3)     NOT NULL DEFAULT 'INR',
    category_id       VARCHAR(36)           NOT NULL,
    brand_id          VARCHAR(36),
    image_url         VARCHAR(500)   NOT NULL,
    rating_average    NUMERIC(3, 2)  NOT NULL DEFAULT 0,
    rating_count      INTEGER        NOT NULL DEFAULT 0,
    featured          BOOLEAN        NOT NULL DEFAULT FALSE,
    -- Soft delete. Removing the row would orphan the order lines that reference
    -- this product, so "delete" only hides it from the catalogue.
    active            BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP      NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT ck_products_price_positive CHECK (price > 0)
);

-- Indexes chosen from the queries that actually run: the catalogue filters by
-- category or brand, and always restricts to active products ordered by newest.
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_brand ON products (brand_id);
CREATE INDEX idx_products_active_created ON products (active, created_at);

CREATE TABLE product_images (
    id         VARCHAR(36)         NOT NULL,
    product_id VARCHAR(36)         NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INTEGER      NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product ON product_images (product_id);

CREATE TABLE product_specifications (
    id         VARCHAR(36)         NOT NULL,
    product_id VARCHAR(36)         NOT NULL,
    label      VARCHAR(80)  NOT NULL,
    -- `value` and `position` are reserved words in H2 (and `value` is reserved in
    -- standard SQL), so the columns are named explicitly rather than quoted.
    -- Quoting would work but would force every hand-written query to quote them
    -- too, forever.
    spec_value VARCHAR(300) NOT NULL,
    sort_order INTEGER      NOT NULL,
    CONSTRAINT pk_product_specifications PRIMARY KEY (id),
    CONSTRAINT fk_product_specifications_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_specifications_product ON product_specifications (product_id);
