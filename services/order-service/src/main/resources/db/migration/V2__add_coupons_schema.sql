-- Adds coupons table and initial promotion seed data to order_db

CREATE TABLE coupons (
    id               VARCHAR(36)           NOT NULL,
    code             VARCHAR(30)    NOT NULL,
    discount_type    VARCHAR(20)    NOT NULL,
    discount_value   NUMERIC(10, 2) NOT NULL,
    min_order_amount NUMERIC(10, 2) NOT NULL,
    max_discount     NUMERIC(10, 2),
    usage_limit      INTEGER,
    usage_count      INTEGER        NOT NULL DEFAULT 0,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    expires_at       TIMESTAMP,
    created_at       TIMESTAMP      NOT NULL,
    CONSTRAINT pk_coupons PRIMARY KEY (id),
    CONSTRAINT uq_coupons_code UNIQUE (code)
);

CREATE INDEX idx_coupons_code ON coupons (code);

-- Seed demo promotional coupons
INSERT INTO coupons (id, code, discount_type, discount_value, min_order_amount, max_discount, usage_limit, usage_count, active, expires_at, created_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'WELCOME10', 'PERCENTAGE', 10.00, 499.00, 500.00, 1000, 0, TRUE, '2030-12-31 23:59:59', CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000002', 'SAVE20', 'PERCENTAGE', 20.00, 1999.00, 1000.00, 500, 0, TRUE, '2030-12-31 23:59:59', CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000003', 'FREESHIP', 'FREE_SHIPPING', 79.00, 0.00, 79.00, 1000, 0, TRUE, '2030-12-31 23:59:59', CURRENT_TIMESTAMP);
