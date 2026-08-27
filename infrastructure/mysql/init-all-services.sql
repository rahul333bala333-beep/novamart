-- ===========================================================================
-- NOVA MART / ZENO MART — Master MySQL Microservices Schema & Seed Script
-- ===========================================================================
-- Host: 127.0.0.1:3306 (or localhost:3306)
-- Root User: root / zeno7925
-- App User: novamart / zeno7925 (and novamart123)
-- Architecture: Database-per-Service Isolation Pattern (7 Microservices)
-- ===========================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 0. User & Privileges Setup
-- ---------------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'novamart'@'%' IDENTIFIED BY 'zeno7925';
CREATE USER IF NOT EXISTS 'novamart'@'localhost' IDENTIFIED BY 'zeno7925';
ALTER USER 'novamart'@'%' IDENTIFIED BY 'zeno7925';
ALTER USER 'novamart'@'localhost' IDENTIFIED BY 'zeno7925';

-- ===========================================================================
-- 1. AUTH SERVICE (auth_db / zeno_auth)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS zeno_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON auth_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON auth_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_auth.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_auth.* TO 'novamart'@'localhost';

USE auth_db;

DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id            VARCHAR(36)  NOT NULL,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(60)  NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role    VARCHAR(30) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE addresses (
    id             VARCHAR(36)  NOT NULL,
    user_id        VARCHAR(36)  NOT NULL,
    label          VARCHAR(40)  NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    line1          VARCHAR(200) NOT NULL,
    line2          VARCHAR(200),
    city           VARCHAR(80)  NOT NULL,
    state          VARCHAR(80)  NOT NULL,
    postal_code    VARCHAR(16)  NOT NULL,
    country        VARCHAR(80)  NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_addresses PRIMARY KEY (id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_addresses_user ON addresses (user_id);

CREATE TABLE refresh_tokens (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- Seed Auth Data (Demo accounts)
INSERT INTO users (id, first_name, last_name, email, password_hash, phone, enabled, created_at, updated_at) VALUES
  ('11111111-1111-4111-8111-111111111111', 'Priya',  'Raghavan', 'admin@novamart.dev', '$2a$10$aw4kXeijzIB0KCP9pY9DtOcbB4Hm4l8jcgPCH7hDCVh0ujEKL80F2', '+91 98450 11001', TRUE, '2026-01-05 09:00:00', '2026-01-05 09:00:00'),
  ('22222222-2222-4222-8222-222222222222', 'Ananya', 'Iyer',     'demo@novamart.dev',  '$2a$10$s3vatcbNv79hESK03Qb99OX0b45iF/JwtxaJZBpR8m299pXtnXXIa',  '+91 98450 22002', TRUE, '2026-01-08 11:20:00', '2026-01-08 11:20:00'),
  ('33333333-3333-4333-8333-333333333333', 'Rohan',  'Mehta',    'rohan@example.com',  '$2a$10$rXdWINt2gBSQqrBX9Hfc6OMWLFfyE/ljHeL3GapRilDjofEKxue/G',  '+91 98450 33003', TRUE, '2026-01-14 16:45:00', '2026-01-14 16:45:00'),
  ('44444444-4444-4444-8444-444444444444', 'Meera',  'Krishnan', 'meera@example.com',  '$2a$10$rXdWINt2gBSQqrBX9Hfc6OMWLFfyE/ljHeL3GapRilDjofEKxue/G',  '+91 98450 44004', TRUE, '2026-01-21 08:05:00', '2026-01-21 08:05:00');

INSERT INTO user_roles (user_id, role) VALUES
  ('11111111-1111-4111-8111-111111111111', 'ADMIN'),
  ('22222222-2222-4222-8222-222222222222', 'USER'),
  ('33333333-3333-4333-8333-333333333333', 'USER'),
  ('44444444-4444-4444-8444-444444444444', 'USER');

INSERT INTO addresses (id, user_id, label, recipient_name, phone, line1, line2, city, state, postal_code, country, is_default, created_at) VALUES
  ('a1111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222', 'Home',   'Ananya Iyer',    '+91 98450 22002', '14 Brigade Gardens', '2nd Cross, Koramangala',  'Bengaluru', 'Karnataka',   '560034', 'India', TRUE,  '2026-01-08 11:30:00'),
  ('a2222222-2222-4222-8222-222222222222', '22222222-2222-4222-8222-222222222222', 'Office', 'Ananya Iyer',    '+91 98450 22002', 'Tower B, Embassy Tech Park', 'Outer Ring Road',    'Bengaluru', 'Karnataka',   '560103', 'India', FALSE, '2026-01-09 09:15:00'),
  ('a3333333-3333-4333-8333-333333333333', '33333333-3333-4333-8333-333333333333', 'Home',   'Rohan Mehta',    '+91 98450 33003', '27 Carter Road',      'Bandra West',            'Mumbai',    'Maharashtra', '400050', 'India', TRUE,  '2026-01-14 17:00:00'),
  ('a4444444-4444-4444-8444-444444444444', '44444444-4444-4444-8444-444444444444', 'Home',   'Meera Krishnan', '+91 98450 44004', '8 Alwarpet Street',   NULL,                     'Chennai',   'Tamil Nadu',  '600018', 'India', TRUE,  '2026-01-21 08:20:00');

-- Flyway metadata for auth_db
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

TRUNCATE TABLE flyway_schema_history;
INSERT INTO flyway_schema_history VALUES
(1, '1', 'create identity schema', 'SQL', 'V1__create_identity_schema.sql', -1791442060, 'root', NOW(), 30, 1),
(2, '2', 'seed demo accounts', 'SQL', 'V2__seed_demo_accounts.sql', -1659795648, 'root', NOW(), 20, 1);


-- ===========================================================================
-- 2. PRODUCT SERVICE (product_db / zeno_products)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS zeno_products CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON product_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON product_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_products.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_products.* TO 'novamart'@'localhost';

USE product_db;

DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS product_reviews;
DROP TABLE IF EXISTS product_specifications;
DROP TABLE IF EXISTS product_images;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS brands;
DROP TABLE IF EXISTS categories;

CREATE TABLE categories (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url   VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE brands (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(80)  NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uq_brands_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id                VARCHAR(36)    NOT NULL,
    sku               VARCHAR(40)    NOT NULL,
    slug              VARCHAR(220)   NOT NULL,
    name              VARCHAR(180)   NOT NULL,
    short_description VARCHAR(300),
    description       TEXT           NOT NULL,
    price             NUMERIC(12, 2) NOT NULL,
    compare_at_price  NUMERIC(12, 2),
    currency          VARCHAR(3)     NOT NULL DEFAULT 'INR',
    category_id       VARCHAR(36)    NOT NULL,
    brand_id          VARCHAR(36),
    image_url         VARCHAR(500)   NOT NULL,
    rating_average    NUMERIC(3, 2)  NOT NULL DEFAULT 0,
    rating_count      INTEGER        NOT NULL DEFAULT 0,
    featured          BOOLEAN        NOT NULL DEFAULT FALSE,
    active            BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP      NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT ck_products_price_positive CHECK (price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_brand ON products (brand_id);
CREATE INDEX idx_products_active_created ON products (active, created_at);

CREATE TABLE product_images (
    id         VARCHAR(36)  NOT NULL,
    product_id VARCHAR(36)  NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INTEGER      NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_product_images_product ON product_images (product_id);

CREATE TABLE product_specifications (
    id         VARCHAR(36)  NOT NULL,
    product_id VARCHAR(36)  NOT NULL,
    label      VARCHAR(80)  NOT NULL,
    spec_value VARCHAR(300) NOT NULL,
    sort_order INTEGER      NOT NULL,
    CONSTRAINT pk_product_specifications PRIMARY KEY (id),
    CONSTRAINT fk_product_specifications_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_product_specifications_product ON product_specifications (product_id);

CREATE TABLE product_reviews (
    id                VARCHAR(36)   NOT NULL,
    product_id        VARCHAR(36)   NOT NULL,
    user_id           VARCHAR(36)   NOT NULL,
    user_name         VARCHAR(120)  NOT NULL,
    rating            INTEGER       NOT NULL,
    title             VARCHAR(180)  NOT NULL,
    comment           TEXT          NOT NULL,
    verified_purchase BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    CONSTRAINT pk_product_reviews PRIMARY KEY (id),
    CONSTRAINT uq_reviews_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_reviews_rating_range CHECK (rating >= 1 AND rating <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_reviews_product ON product_reviews (product_id);
CREATE INDEX idx_reviews_user ON product_reviews (user_id);

-- Seed Categories
INSERT INTO categories (id, name, slug, description, image_url, created_at) VALUES
  ('7a83a44e-7ea9-588d-90a9-c6f30b1ed62f', 'Audio', 'audio', 'Headphones, earbuds and speakers tuned for everyday listening and critical work.', 'https://loremflickr.com/800/600/headphones,audio?lock=87', '2026-01-02 09:00:00'),
  ('06610ee3-a46b-5738-adfd-4ae2b5814604', 'Computing', 'computing', 'Laptops, displays, storage and the peripherals that surround them.', 'https://loremflickr.com/800/600/laptop,computer?lock=415', '2026-01-02 09:00:00'),
  ('4cdd788c-eb64-5767-b65f-9606f83b2bcb', 'Mobile & Tablets', 'mobile-tablets', 'Phones, tablets and the accessories that keep them running.', 'https://loremflickr.com/800/600/smartphone,tablet?lock=549', '2026-01-02 09:00:00'),
  ('9d51aa3b-8168-5cd8-a37c-8df2604fe561', 'Wearables', 'wearables', 'Smartwatches and fitness trackers built for all-day wear.', 'https://loremflickr.com/800/600/smartwatch,wearable?lock=827', '2026-01-02 09:00:00'),
  ('1b5a23ae-8705-55c1-a9a3-537afe033713', 'Home & Kitchen', 'home-kitchen', 'Considered appliances for the kitchen counter and the living room.', 'https://loremflickr.com/800/600/kitchen,appliance?lock=386', '2026-01-02 09:00:00'),
  ('1a79b9fd-2436-5f30-8f4c-de6d3d65b383', 'Photography', 'photography', 'Cameras and lenses for people who shoot deliberately.', 'https://loremflickr.com/800/600/camera,photography?lock=757', '2026-01-02 09:00:00'),
  ('8740d7f6-b451-5b59-b10b-124aa0e8a53d', 'Gaming', 'gaming', 'Controllers, headsets and gear for long sessions.', 'https://loremflickr.com/800/600/gaming,controller?lock=452', '2026-01-02 09:00:00');

-- Seed Brands
INSERT INTO brands (id, name, slug, created_at) VALUES
  ('417365a8-31ea-5bc6-b24b-84b8ad8f9bfc', 'Aurelia Audio', 'aurelia-audio', '2026-01-02 09:05:00'),
  ('bce54fa9-f9ed-57c2-9700-677ca6e97788', 'Vantage', 'vantage', '2026-01-02 09:05:00'),
  ('412add93-8c76-50ad-acd0-2ec76c6b3634', 'Nordkraft', 'nordkraft', '2026-01-02 09:05:00'),
  ('b38df3e2-81ab-58f4-9da4-95b566fa56c5', 'Lumen', 'lumen', '2026-01-02 09:05:00'),
  ('eb1ffc4e-bf81-5499-814a-b15d2b125406', 'Corvex', 'corvex', '2026-01-02 09:05:00'),
  ('cf291fcc-c7b1-530b-af85-9d825606e157', 'Halcyon', 'halcyon', '2026-01-02 09:05:00'),
  ('dc7b8f5b-458f-50fe-8bb3-883755145bff', 'Meridian Labs', 'meridian-labs', '2026-01-02 09:05:00'),
  ('9a164065-b228-5f1b-b13c-a17d29076747', 'Kestrel', 'kestrel', '2026-01-02 09:05:00');

-- Seed 25 Core Luxury Tech Products
INSERT INTO products (id, sku, slug, name, short_description, description, price, compare_at_price, currency, category_id, brand_id, image_url, rating_average, rating_count, featured, active, created_at, updated_at) VALUES
  ('343213de-c447-56c6-ac74-dd29fcff1fec', 'AUR-HALO-BLK', 'aurelia-halo-noise-cancelling-headphones', 'Aurelia Halo Noise-Cancelling Headphones', 'Over-ear ANC headphones with 40-hour battery and memory-foam cushions.', 'The Halo pairs a two-stage adaptive noise-cancelling system with 40mm bio-cellulose drivers. Commuter rumble disappears with 40-hour continuous playback.', 18999.00, 24999.00, 'INR', '7a83a44e-7ea9-588d-90a9-c6f30b1ed62f', '417365a8-31ea-5bc6-b24b-84b8ad8f9bfc', 'https://loremflickr.com/900/900/headphones?lock=310', 4.70, 2143, TRUE, TRUE, '2026-01-03 10:21:00', '2026-01-03 10:21:00'),
  ('06e48299-d406-5f29-bcee-728d902d65ea', 'AUR-DRIFT-WHT', 'aurelia-drift-wireless-earbuds', 'Aurelia Drift Wireless Earbuds', 'Compact true-wireless earbuds with a pocketable charging case.', 'Drift is built around comfort over commotion: a 5.4mm driver in a lightweight shell. IPX5 water resistance with three full charges.', 6499.00, 8999.00, 'INR', '7a83a44e-7ea9-588d-90a9-c6f30b1ed62f', '417365a8-31ea-5bc6-b24b-84b8ad8f9bfc', 'https://loremflickr.com/900/900/earbuds,wireless?lock=847', 4.40, 1876, TRUE, TRUE, '2026-01-04 10:28:00', '2026-01-04 10:28:00'),
  ('f4a53e54-9efb-570e-a0a9-59cc67112fce', 'NDK-RESON-WAL', 'nordkraft-resonance-bookshelf-speakers', 'Nordkraft Resonance Bookshelf Speakers', 'Two-way passive bookshelf pair in a real walnut veneer cabinet.', 'A 25mm soft-dome tweeter over a 130mm paper-cone woofer in a braced walnut cabinet. Uncompromised acoustic tuning.', 27499.00, NULL, 'INR', '7a83a44e-7ea9-588d-90a9-c6f30b1ed62f', '412add93-8c76-50ad-acd0-2ec76c6b3634', 'https://loremflickr.com/900/900/speakers,bookshelf?lock=824', 4.60, 412, FALSE, TRUE, '2026-01-05 10:35:00', '2026-01-05 10:35:00'),
  ('021f37b8-99e8-574a-8604-5c586e93b3e8', 'HAL-MON-PAIR', 'halcyon-studio-monitor-pair', 'Halcyon Studio Monitor Pair', 'Active nearfield monitors with room-correction trim controls.', 'Bi-amplified nearfields intended for a studio desk. 60W woofer amp and 30W tweeter amp with balanced XLR inputs.', 34999.00, 39999.00, 'INR', '7a83a44e-7ea9-588d-90a9-c6f30b1ed62f', 'cf291fcc-c7b1-530b-af85-9d825606e157', 'https://loremflickr.com/900/900/studio,monitor,speaker?lock=114', 4.80, 287, FALSE, TRUE, '2026-01-06 10:42:00', '2026-01-06 10:42:00'),
  ('2d4bc1ef-ba0d-514f-be5d-eac47ce54120', 'VNT-MER14-SLV', 'vantage-meridian-14-ultrabook', 'Vantage Meridian 14 Ultrabook', '14-inch magnesium ultrabook, 1.19 kg, 18-hour battery.', 'Magnesium-alloy chassis keeps weight under 1.19 kg. 14-inch 2.8K 120Hz display with Thunderbolt 4 power delivery.', 118999.00, 129999.00, 'INR', '06610ee3-a46b-5738-adfd-4ae2b5814604', 'bce54fa9-f9ed-57c2-9700-677ca6e97788', 'https://loremflickr.com/900/900/laptop,ultrabook?lock=551', 4.50, 934, TRUE, TRUE, '2026-01-07 10:49:00', '2026-01-07 10:49:00'),
  ('4a74101a-ff9e-57ba-abd3-b01b84e58e96', 'VNT-FRG16-GRY', 'vantage-forge-16-creator-laptop', 'Vantage Forge 16 Creator Laptop', '16-inch workstation with discrete graphics and a colour-accurate panel.', 'Vapour-chamber cooling, discrete graphics, and 100% DCI-P3 factory-calibrated panel for professional creative workflows.', 189999.00, NULL, 'INR', '06610ee3-a46b-5738-adfd-4ae2b5814604', 'bce54fa9-f9ed-57c2-9700-677ca6e97788', 'https://loremflickr.com/900/900/laptop,workstation?lock=19', 4.60, 521, TRUE, TRUE, '2026-01-08 10:56:00', '2026-01-08 10:56:00'),
  ('33c6f1f3-4e76-54ad-95e9-38692dba8f00', 'CVX-APEX-KB', 'corvex-apex-mechanical-keyboard', 'Corvex Apex Mechanical Keyboard', '75% hot-swappable mechanical keyboard with a gasket mount.', 'Gasket-mounted acoustic dampening with hot-swappable switch sockets and CNC aluminium body.', 12499.00, 14999.00, 'INR', '06610ee3-a46b-5738-adfd-4ae2b5814604', 'eb1ffc4e-bf81-5499-814a-b15d2b125406', 'https://loremflickr.com/900/900/keyboard,mechanical?lock=575', 4.70, 1654, FALSE, TRUE, '2026-01-09 10:03:00', '2026-01-09 10:03:00'),
  ('6442b5d0-4b9a-5b67-be20-cb6eb3f3b81f', 'CVX-GLIDE-BLK', 'corvex-glide-wireless-mouse', 'Corvex Glide Wireless Mouse', 'Lightweight 58g wireless mouse with a 26K sensor.', '58g ultra-lightweight solid shell design, 26,000 DPI optical sensor, and 90-hour battery life.', 4299.00, NULL, 'INR', '06610ee3-a46b-5738-adfd-4ae2b5814604', 'eb1ffc4e-bf81-5499-814a-b15d2b125406', 'https://loremflickr.com/900/900/mouse,computer?lock=705', 4.30, 2087, FALSE, TRUE, '2026-01-10 10:10:00', '2026-01-10 10:10:00'),
  ('03fdb501-0e02-5b63-bda8-a9e65a61dec4', 'LUM-CLR27-4K', 'lumen-clarity-27-4k-monitor', 'Lumen Clarity 27 4K Monitor', '27-inch 4K IPS display with USB-C power delivery.', 'Ultra-crisp 163 PPI 4K IPS display with 90W USB-C PD hub and fully ergonomic four-way stand.', 42999.00, 49999.00, 'INR', '06610ee3-a46b-5738-adfd-4ae2b5814604', 'b38df3e2-81ab-58f4-9da4-95b566fa56c5', 'https://loremflickr.com/900/900/monitor,display?lock=647', 4.50, 743, TRUE, TRUE, '2026-01-11 10:17:00', '2026-01-11 10:17:00'),
  ('42427033-98d7-5020-a92f-49c6bdf9aa0c', 'NDK-VLT-2TB', 'nordkraft-vault-2tb-nvme-ssd', 'Nordkraft Vault 2TB NVMe SSD', 'PCIe 4.0 internal SSD with a five-year warranty.', '7,300 MB/s sequential speeds, dedicated DRAM cache, and graphene thermal dissipation layer.', 15999.00, NULL, 'INR', '06610ee3-a46b-5738-adfd-4ae2b5814604', '412add93-8c76-50ad-acd0-2ec76c6b3634', 'https://loremflickr.com/900/900/ssd,storage?lock=249', 4.60, 1129, FALSE, TRUE, '2026-01-12 10:24:00', '2026-01-12 10:24:00'),
  ('b41c22a0-cbb0-5c00-9aa3-0a7925db296d', 'VNT-PLS5G-BLU', 'vantage-pulse-5g-smartphone', 'Vantage Pulse 5G Smartphone', '6.5-inch 5G phone with a 5000mAh cell and three-year updates.', '6.5-inch 120Hz OLED screen, 5000mAh battery, and 50MP OIS camera with 4 years of security support.', 54999.00, 61999.00, 'INR', '4cdd788c-eb64-5767-b65f-9606f83b2bcb', 'bce54fa9-f9ed-57c2-9700-677ca6e97788', 'https://loremflickr.com/900/900/smartphone,phone?lock=495', 4.40, 3211, TRUE, TRUE, '2026-01-13 10:31:00', '2026-01-13 10:31:00'),
  ('54dcfc85-e51c-5515-a8ce-7f11e2e34d30', 'VNT-PLSPRO-BLK', 'vantage-pulse-pro-5g', 'Vantage Pulse Pro 5G', 'Flagship 6.8-inch phone with a periscope telephoto.', 'Titanium frame, 5x periscope telephoto lens, 2600 nits peak display brightness, and 80W turbo charge.', 89999.00, NULL, 'INR', '4cdd788c-eb64-5767-b65f-9606f83b2bcb', 'bce54fa9-f9ed-57c2-9700-677ca6e97788', 'https://loremflickr.com/900/900/smartphone,flagship?lock=865', 4.60, 1487, FALSE, TRUE, '2026-01-14 10:38:00', '2026-01-14 10:38:00'),
  ('49e2b7fd-7d8f-534e-83ab-970e6b7e648d', 'LUM-SLT11', 'lumen-slate-11-tablet', 'Lumen Slate 11 Tablet', '11-inch tablet with stylus support and a laminated display.', 'Fully laminated 11-inch display with precision stylus tracking, 16-hour endurance, and quad-speaker arrays.', 38999.00, NULL, 'INR', '4cdd788c-eb64-5767-b65f-9606f83b2bcb', 'b38df3e2-81ab-58f4-9da4-95b566fa56c5', 'https://loremflickr.com/900/900/tablet?lock=703', 4.30, 664, FALSE, TRUE, '2026-01-15 10:45:00', '2026-01-15 10:45:00'),
  ('29dfc8bb-fbfe-5c2c-94c6-d7b007dfeab6', 'KST-PWR20K', 'kestrel-powercell-20k-power-bank', 'Kestrel PowerCell 20K Power Bank', '20,000mAh power bank with 65W USB-C output.', '65W high-speed USB-C Power Delivery with real-time numeric capacity display.', 3499.00, 4299.00, 'INR', '4cdd788c-eb64-5767-b65f-9606f83b2bcb', '9a164065-b228-5f1b-b13c-a17d29076747', 'https://loremflickr.com/900/900/powerbank,charger?lock=567', 4.50, 2934, FALSE, TRUE, '2026-01-16 10:52:00', '2026-01-16 10:52:00'),
  ('c92604ae-a1c3-5a0e-9850-aa313fa0b2ae', 'MRD-ARC-SLV', 'meridian-labs-arc-smartwatch', 'Meridian Labs Arc Smartwatch', 'Aluminium smartwatch with 7-day battery and built-in GPS.', 'Aircraft-grade aluminium case, dual-frequency multi-satellite GPS, and clinical-grade sleep metrics.', 24999.00, 28999.00, 'INR', '9d51aa3b-8168-5cd8-a37c-8df2604fe561', 'dc7b8f5b-458f-50fe-8bb3-883755145bff', 'https://loremflickr.com/900/900/smartwatch,watch?lock=504', 4.50, 1298, TRUE, TRUE, '2026-01-17 10:59:00', '2026-01-17 10:59:00'),
  ('e4dd746f-c87a-53ab-8b65-fa3f642c041a', 'MRD-TRC-BLK', 'meridian-labs-trace-fitness-band', 'Meridian Labs Trace Fitness Band', 'Slim fitness band with 14-day battery life.', 'Ultra-slim lightweight design, 14-day endurance, and automatic workout detection.', 4999.00, NULL, 'INR', '9d51aa3b-8168-5cd8-a37c-8df2604fe561', 'dc7b8f5b-458f-50fe-8bb3-883755145bff', 'https://loremflickr.com/900/900/fitness,band,tracker?lock=542', 4.20, 3567, FALSE, TRUE, '2026-01-18 10:06:00', '2026-01-18 10:06:00'),
  ('132f0d76-19d4-5f94-9408-d8837e1106da', 'AUR-SPRNT-GRN', 'aurelia-sprint-sport-earbuds', 'Aurelia Sprint Sport Earbuds', 'Ear-hook sport earbuds rated IP67 with a secure fit.', 'Ergonomic moldable ear-hooks, IP67 waterproof enclosure, and responsive ambient passthrough mode.', 5499.00, NULL, 'INR', '9d51aa3b-8168-5cd8-a37c-8df2604fe561', '417365a8-31ea-5bc6-b24b-84b8ad8f9bfc', 'https://loremflickr.com/900/900/earbuds,sport,running?lock=691', 4.30, 908, FALSE, TRUE, '2026-01-19 10:13:00', '2026-01-19 10:13:00'),
  ('b7e96173-0c4f-5771-95dd-a9129ef21d3e', 'HAL-EMBR-STL', 'halcyon-ember-espresso-machine', 'Halcyon Ember Espresso Machine', 'Dual-boiler espresso machine with PID temperature control.', 'Dual independent stainless boilers, commercial PID thermal control, and rotary pressure monitoring.', 46999.00, 54999.00, 'INR', '1b5a23ae-8705-55c1-a9a3-537afe033713', 'cf291fcc-c7b1-530b-af85-9d825606e157', 'https://loremflickr.com/900/900/espresso,coffee,machine?lock=661', 4.60, 476, TRUE, TRUE, '2026-01-20 10:20:00', '2026-01-20 10:20:00'),
  ('1049f1db-f249-551b-a0fe-838e88d5085f', 'HAL-BREW-KTL', 'halcyon-brew-precision-kettle', 'Halcyon Brew Precision Kettle', 'Gooseneck kettle with variable temperature and a hold timer.', 'Precision gooseneck pour spout with 1-degree temperature adjustment and 60-minute holding heat.', 8999.00, NULL, 'INR', '1b5a23ae-8705-55c1-a9a3-537afe033713', 'cf291fcc-c7b1-530b-af85-9d825606e157', 'https://loremflickr.com/900/900/kettle,coffee,pourover?lock=875', 4.70, 1342, FALSE, TRUE, '2026-01-21 10:27:00', '2026-01-21 10:27:00'),
  ('c794c12d-44ee-5378-913c-a2012877c992', 'NDK-PURE-AIR', 'nordkraft-pure-air-purifier', 'Nordkraft Pure Air Purifier', 'HEPA-13 purifier rated for rooms up to 60 square metres.', 'Medical-grade true HEPA-13 element, activated carbon layer, and silent 24dB sleep operation.', 32999.00, 37999.00, 'INR', '1b5a23ae-8705-55c1-a9a3-537afe033713', '412add93-8c76-50ad-acd0-2ec76c6b3634', 'https://loremflickr.com/900/900/airpurifier,home?lock=152', 4.40, 623, FALSE, TRUE, '2026-01-22 10:34:00', '2026-01-22 10:34:00'),
  ('eeff95a6-fb55-5c76-ab0d-c9696b591527', 'KST-LUMDSK', 'kestrel-lumen-desk-lamp', 'Kestrel Lumen Desk Lamp', 'Asymmetric LED desk lamp with tunable colour temperature.', 'Glaring-free asymmetric beam design with continuous 2700K-6000K colour temperature tuning.', 6499.00, NULL, 'INR', '1b5a23ae-8705-55c1-a9a3-537afe033713', '9a164065-b228-5f1b-b13c-a17d29076747', 'https://loremflickr.com/900/900/desklamp,light?lock=582', 4.40, 851, FALSE, TRUE, '2026-01-23 10:41:00', '2026-01-23 10:41:00'),
  ('2a6528cc-98f5-5a4a-aebf-3d9e6b94a520', 'LUM-VSTA-BODY', 'lumen-vista-mirrorless-camera-body', 'Lumen Vista Mirrorless Camera Body', 'Full-frame 33MP mirrorless body with in-body stabilisation.', '33MP full-frame sensor, 5-axis 7-stop sensor shift stabilisation, and dual UHS-II slots.', 134999.00, 149999.00, 'INR', '1a79b9fd-2436-5f30-8f4c-de6d3d65b383', 'b38df3e2-81ab-58f4-9da4-95b566fa56c5', 'https://loremflickr.com/900/900/camera,mirrorless?lock=531', 4.80, 392, TRUE, TRUE, '2026-01-24 10:48:00', '2026-01-24 10:48:00'),
  ('5fae9de6-5fd8-5c89-b9bb-a40924a143d1', 'LUM-VSTA-35', 'lumen-vista-35mm-f-1-8-lens', 'Lumen Vista 35mm f/1.8 Lens', 'Compact full-frame 35mm prime with weather sealing.', 'Razor-sharp f/1.8 wide aperture with silent linear AF motors and all-weather sealing.', 42999.00, NULL, 'INR', '1a79b9fd-2436-5f30-8f4c-de6d3d65b383', 'b38df3e2-81ab-58f4-9da4-95b566fa56c5', 'https://loremflickr.com/900/900/cameralens,photography?lock=506', 4.70, 268, FALSE, TRUE, '2026-01-25 10:55:00', '2026-01-25 10:55:00'),
  ('a9873b9f-ead9-57d9-b4e4-07b3be71de45', 'CVX-RIFT-CTL', 'corvex-rift-wireless-controller', 'Corvex Rift Wireless Controller', 'Wireless controller with hall-effect sticks and back paddles.', 'Drift-free Hall-effect magnetic analog sticks, 4 customizable back paddles, and tri-mode wireless.', 6999.00, 8499.00, 'INR', '8740d7f6-b451-5b59-b10b-124aa0e8a53d', 'eb1ffc4e-bf81-5499-814a-b15d2b125406', 'https://loremflickr.com/900/900/gamecontroller,gaming?lock=880', 4.50, 2456, FALSE, TRUE, '2026-01-26 10:02:00', '2026-01-26 10:02:00'),
  ('7a42ea07-0de4-583e-b059-a5e2349fd3c9', 'CVX-VCTR-HS', 'corvex-vector-gaming-headset', 'Corvex Vector Gaming Headset', 'Wireless gaming headset with a broadcast-grade boom microphone.', 'Broadcast-grade cardioid microphone, 50mm custom neodymium drivers, and low-latency 2.4GHz audio.', 11999.00, NULL, 'INR', '8740d7f6-b451-5b59-b10b-124aa0e8a53d', 'eb1ffc4e-bf81-5499-814a-b15d2b125406', 'https://loremflickr.com/900/900/gamingheadset,headphones?lock=905', 4.40, 1105, FALSE, TRUE, '2026-01-27 10:09:00', '2026-01-27 10:09:00');

-- Flyway metadata for product_db
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

TRUNCATE TABLE flyway_schema_history;
INSERT INTO flyway_schema_history VALUES
(1, '1', 'create catalogue schema', 'SQL', 'V1__create_catalogue_schema.sql', -1838841021, 'root', NOW(), 45, 1),
(2, '2', 'seed demo catalogue', 'SQL', 'V2__seed_demo_catalogue.sql', 174092288, 'root', NOW(), 120, 1),
(3, '3', 'add reviews and ratings schema', 'SQL', 'V3__add_reviews_and_ratings_schema.sql', 549321876, 'root', NOW(), 15, 1);


-- ===========================================================================
-- 3. CART SERVICE (cart_db / zeno_cart)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS zeno_cart CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON cart_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON cart_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_cart.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_cart.* TO 'novamart'@'localhost';

USE cart_db;

DROP TABLE IF EXISTS wishlist_items;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;

CREATE TABLE carts (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT uq_carts_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
    id         VARCHAR(36) NOT NULL,
    cart_id    VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity   INTEGER     NOT NULL,
    added_at   TIMESTAMP   NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

CREATE TABLE wishlist_items (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_wishlist_items PRIMARY KEY (id),
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_wishlist_user ON wishlist_items (user_id);

-- Flyway metadata for cart_db
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

TRUNCATE TABLE flyway_schema_history;
INSERT INTO flyway_schema_history VALUES
(1, '1', 'create cart schema', 'SQL', 'V1__create_cart_schema.sql', 123456789, 'root', NOW(), 20, 1),
(2, '2', 'add wishlist schema', 'SQL', 'V2__add_wishlist_schema.sql', 987654321, 'root', NOW(), 15, 1);


-- ===========================================================================
-- 4. ORDER SERVICE (order_db / zeno_orders)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS zeno_orders CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON order_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_orders.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_orders.* TO 'novamart'@'localhost';

USE order_db;

DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS order_number_counter;
DROP TABLE IF EXISTS order_events;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id                      VARCHAR(36)    NOT NULL,
    order_number            VARCHAR(30)    NOT NULL,
    user_id                 VARCHAR(36)    NOT NULL,
    status                  VARCHAR(20)    NOT NULL,
    subtotal                NUMERIC(12, 2) NOT NULL,
    delivery_fee            NUMERIC(12, 2) NOT NULL,
    discount                NUMERIC(12, 2) NOT NULL,
    total                   NUMERIC(12, 2) NOT NULL,
    currency                VARCHAR(3)     NOT NULL,
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
    CONSTRAINT uq_orders_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_orders_total_non_negative CHECK (total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_orders_user_placed ON orders (user_id, placed_at);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id         VARCHAR(36)    NOT NULL,
    order_id   VARCHAR(36)    NOT NULL,
    product_id VARCHAR(36)    NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE order_events (
    id          VARCHAR(36)  NOT NULL,
    order_id    VARCHAR(36)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    note        VARCHAR(500),
    occurred_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_order_events PRIMARY KEY (id),
    CONSTRAINT fk_order_events_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_order_events_order ON order_events (order_id);

CREATE TABLE order_number_counter (
    id         INTEGER NOT NULL,
    next_value BIGINT  NOT NULL,
    CONSTRAINT pk_order_number_counter PRIMARY KEY (id),
    CONSTRAINT ck_order_number_counter_single_row CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO order_number_counter (id, next_value) VALUES (1, 1001);

CREATE TABLE coupons (
    id               VARCHAR(36)    NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_coupons_code ON coupons (code);

INSERT INTO coupons (id, code, discount_type, discount_value, min_order_amount, max_discount, usage_limit, usage_count, active, expires_at, created_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'WELCOME10', 'PERCENTAGE', 10.00, 499.00, 500.00, 1000, 0, TRUE, '2030-12-31 23:59:59', NOW()),
    ('c0000000-0000-0000-0000-000000000002', 'SAVE20', 'PERCENTAGE', 20.00, 1999.00, 1000.00, 500, 0, TRUE, '2030-12-31 23:59:59', NOW()),
    ('c0000000-0000-0000-0000-000000000003', 'FREESHIP', 'FREE_SHIPPING', 79.00, 0.00, 79.00, 1000, 0, TRUE, '2030-12-31 23:59:59', NOW());

-- Flyway metadata for order_db
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

TRUNCATE TABLE flyway_schema_history;
INSERT INTO flyway_schema_history VALUES
(1, '1', 'create order schema', 'SQL', 'V1__create_order_schema.sql', 1122334455, 'root', NOW(), 30, 1),
(2, '2', 'add coupons schema', 'SQL', 'V2__add_coupons_schema.sql', 554433221, 'root', NOW(), 20, 1);


-- ===========================================================================
-- 5. PAYMENT SERVICE (payment_db / zeno_payments)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS zeno_payments CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON payment_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_payments.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_payments.* TO 'novamart'@'localhost';

USE payment_db;

DROP TABLE IF EXISTS payment_transactions;
DROP TABLE IF EXISTS payments;

CREATE TABLE payments (
    id                    VARCHAR(36)    NOT NULL,
    order_id              VARCHAR(36)    NOT NULL,
    user_id               VARCHAR(36)    NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    method                VARCHAR(30)    NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    transaction_reference VARCHAR(40)    NOT NULL,
    failure_reason        VARCHAR(300),
    created_at            TIMESTAMP      NOT NULL,
    settled_at            TIMESTAMP,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_transaction_ref UNIQUE (transaction_reference),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_user ON payments (user_id);
CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE payment_transactions (
    id                VARCHAR(36)    NOT NULL,
    payment_id        VARCHAR(36)    NOT NULL,
    type              VARCHAR(20)    NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,
    gateway_reference VARCHAR(60)    NOT NULL,
    message           VARCHAR(300),
    occurred_at       TIMESTAMP      NOT NULL,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT fk_payment_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_transactions_payment ON payment_transactions (payment_id);

-- Flyway metadata for payment_db
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

TRUNCATE TABLE flyway_schema_history;
INSERT INTO flyway_schema_history VALUES
(1, '1', 'create payment schema', 'SQL', 'V1__create_payment_schema.sql', 111122223, 'root', NOW(), 25, 1);


-- ===========================================================================
-- 6. INVENTORY SERVICE (inventory_db / zeno_inventory)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS zeno_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON inventory_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_inventory.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_inventory.* TO 'novamart'@'localhost';

USE inventory_db;

DROP TABLE IF EXISTS stock_transactions;
DROP TABLE IF EXISTS inventory_items;

CREATE TABLE inventory_items (
    product_id        VARCHAR(36) NOT NULL,
    total_quantity    INTEGER     NOT NULL,
    reserved_quantity INTEGER     NOT NULL DEFAULT 0,
    reorder_threshold INTEGER     NOT NULL DEFAULT 5,
    version           BIGINT      NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP   NOT NULL,
    CONSTRAINT pk_inventory_items PRIMARY KEY (product_id),
    CONSTRAINT ck_inventory_non_negative CHECK (total_quantity >= 0 AND reserved_quantity >= 0),
    CONSTRAINT ck_inventory_reserved_within_total CHECK (reserved_quantity <= total_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_transactions (
    id                 VARCHAR(36) NOT NULL,
    product_id         VARCHAR(36) NOT NULL,
    type               VARCHAR(30) NOT NULL,
    quantity           INTEGER     NOT NULL,
    resulting_total    INTEGER     NOT NULL,
    resulting_reserved INTEGER     NOT NULL,
    reference_id       VARCHAR(80),
    occurred_at        TIMESTAMP   NOT NULL,
    CONSTRAINT pk_stock_transactions PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_stock_transactions_product ON stock_transactions (product_id);
CREATE INDEX idx_stock_transactions_reference ON stock_transactions (reference_id);

-- Seed Stock Levels Matching 25 Products
INSERT INTO inventory_items (product_id, total_quantity, reserved_quantity, reorder_threshold, version, updated_at) VALUES
  ('343213de-c447-56c6-ac74-dd29fcff1fec', 42, 0, 5, 0, '2026-01-28 12:00:00'),
  ('06e48299-d406-5f29-bcee-728d902d65ea', 96, 0, 5, 0, '2026-01-28 12:00:00'),
  ('f4a53e54-9efb-570e-a0a9-59cc67112fce', 18, 0, 5, 0, '2026-01-28 12:00:00'),
  ('021f37b8-99e8-574a-8604-5c586e93b3e8', 9, 0, 5, 0, '2026-01-28 12:00:00'),
  ('2d4bc1ef-ba0d-514f-be5d-eac47ce54120', 24, 0, 5, 0, '2026-01-28 12:00:00'),
  ('4a74101a-ff9e-57ba-abd3-b01b84e58e96', 12, 0, 5, 0, '2026-01-28 12:00:00'),
  ('33c6f1f3-4e76-54ad-95e9-38692dba8f00', 61, 0, 5, 0, '2026-01-28 12:00:00'),
  ('6442b5d0-4b9a-5b67-be20-cb6eb3f3b81f', 134, 0, 5, 0, '2026-01-28 12:00:00'),
  ('03fdb501-0e02-5b63-bda8-a9e65a61dec4', 21, 0, 5, 0, '2026-01-28 12:00:00'),
  ('42427033-98d7-5020-a92f-49c6bdf9aa0c', 47, 0, 5, 0, '2026-01-28 12:00:00'),
  ('b41c22a0-cbb0-5c00-9aa3-0a7925db296d', 38, 0, 5, 0, '2026-01-28 12:00:00'),
  ('54dcfc85-e51c-5515-a8ce-7f11e2e34d30', 16, 0, 5, 0, '2026-01-28 12:00:00'),
  ('49e2b7fd-7d8f-534e-83ab-970e6b7e648d', 29, 0, 5, 0, '2026-01-28 12:00:00'),
  ('29dfc8bb-fbfe-5c2c-94c6-d7b007dfeab6', 0, 0, 5, 0, '2026-01-28 12:00:00'),
  ('c92604ae-a1c3-5a0e-9850-aa313fa0b2ae', 33, 0, 5, 0, '2026-01-28 12:00:00'),
  ('e4dd746f-c87a-53ab-8b65-fa3f642c041a', 88, 0, 5, 0, '2026-01-28 12:00:00'),
  ('132f0d76-19d4-5f94-9408-d8837e1106da', 71, 0, 5, 0, '2026-01-28 12:00:00'),
  ('b7e96173-0c4f-5771-95dd-a9129ef21d3e', 7, 0, 5, 0, '2026-01-28 12:00:00'),
  ('1049f1db-f249-551b-a0fe-838e88d5085f', 54, 0, 5, 0, '2026-01-28 12:00:00'),
  ('c794c12d-44ee-5378-913c-a2012877c992', 4, 0, 5, 0, '2026-01-28 12:00:00'),
  ('eeff95a6-fb55-5c76-ab0d-c9696b591527', 63, 0, 5, 0, '2026-01-28 12:00:00'),
  ('2a6528cc-98f5-5a4a-aebf-3d9e6b94a520', 8, 0, 5, 0, '2026-01-28 12:00:00'),
  ('5fae9de6-5fd8-5c89-b9bb-a40924a143d1', 15, 0, 5, 0, '2026-01-28 12:00:00'),
  ('a9873b9f-ead9-57d9-b4e4-07b3be71de45', 77, 0, 5, 0, '2026-01-28 12:00:00'),
  ('7a42ea07-0de4-583e-b059-a5e2349fd3c9', 3, 0, 5, 0, '2026-01-28 12:00:00');

-- Flyway metadata for inventory_db
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

TRUNCATE TABLE flyway_schema_history;
INSERT INTO flyway_schema_history VALUES
(1, '1', 'create inventory schema', 'SQL', 'V1__create_inventory_schema.sql', 333344445, 'root', NOW(), 25, 1),
(2, '2', 'seed opening stock', 'SQL', 'V2__seed_opening_stock.sql', 555566667, 'root', NOW(), 35, 1);


-- ===========================================================================
-- 7. NOTIFICATION SERVICE (notification_db / zeno_notifications)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS zeno_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON notification_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_notifications.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_notifications.* TO 'novamart'@'localhost';

USE notification_db;

DROP TABLE IF EXISTS notifications;

CREATE TABLE notifications (
    id             VARCHAR(36)   NOT NULL,
    user_id        VARCHAR(36)   NOT NULL,
    type           VARCHAR(40)   NOT NULL,
    channel        VARCHAR(20)   NOT NULL,
    recipient      VARCHAR(180),
    subject        VARCHAR(200)  NOT NULL,
    body           TEXT          NOT NULL,
    reference_id   VARCHAR(80),
    status         VARCHAR(20)   NOT NULL,
    failure_reason VARCHAR(300),
    is_read        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP     NOT NULL,
    sent_at        TIMESTAMP,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_created ON notifications (created_at);
CREATE INDEX idx_notifications_reference ON notifications (reference_id);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);

-- Flyway metadata for notification_db
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

TRUNCATE TABLE flyway_schema_history;
INSERT INTO flyway_schema_history VALUES
(1, '1', 'create notification schema', 'SQL', 'V1__create_notification_schema.sql', 777788889, 'root', NOW(), 20, 1),
(2, '2', 'add read status to notifications', 'SQL', 'V2__add_read_status_to_notifications.sql', 999900001, 'root', NOW(), 15, 1);

-- ---------------------------------------------------------------------------
-- Finalize Privileges
-- ---------------------------------------------------------------------------
FLUSH PRIVILEGES;
SET FOREIGN_KEY_CHECKS = 1;

-- ===========================================================================
-- 8. USEFUL VERIFICATION / CRUD SAMPLE QUERIES
-- ===========================================================================
-- Query 1: Check all users and their assigned roles
-- SELECT u.id, u.email, u.first_name, u.last_name, r.role FROM auth_db.users u JOIN auth_db.user_roles r ON u.id = r.user_id;

-- Query 2: List top featured products with category & brand
-- SELECT p.name, p.price, c.name AS category, b.name AS brand, p.rating_average FROM product_db.products p JOIN product_db.categories c ON p.category_id = c.id LEFT JOIN product_db.brands b ON p.brand_id = b.id WHERE p.featured = TRUE;

-- Query 3: Check low inventory products (stock <= reorder threshold)
-- SELECT i.product_id, i.total_quantity, i.reserved_quantity, i.reorder_threshold FROM inventory_db.inventory_items i WHERE i.total_quantity <= i.reorder_threshold;

-- Query 4: Active promo coupons
-- SELECT code, discount_type, discount_value, min_order_amount FROM order_db.coupons WHERE active = TRUE;
