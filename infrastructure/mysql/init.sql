-- ===========================================================================
-- ZENO MART — MySQL Multi-Database Initialization Script
-- ===========================================================================
-- Architecture: Microservices Database-per-Service Isolation Pattern
-- Target RDBMS: MySQL 8.0 / 8.4+
-- Charset: utf8mb4 (Full Unicode / Emoji support)
-- Collation: utf8mb4_unicode_ci
-- ===========================================================================

-- 1. Create Dedicated Microservice Databases
CREATE DATABASE IF NOT EXISTS zeno_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_products CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_cart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_orders CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_payments CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Create Aliases / Fallback Databases for Standard Naming Conventions
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. Create Service Application User
CREATE USER IF NOT EXISTS 'novamart'@'%' IDENTIFIED BY 'zeno7925';
CREATE USER IF NOT EXISTS 'novamart'@'localhost' IDENTIFIED BY 'zeno7925';
ALTER USER 'novamart'@'%' IDENTIFIED BY 'zeno7925';
ALTER USER 'novamart'@'localhost' IDENTIFIED BY 'zeno7925';

-- 4. Grant Database Privileges to Service Application User
GRANT ALL PRIVILEGES ON zeno_auth.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_products.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_cart.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_orders.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_payments.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_inventory.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON zeno_notifications.* TO 'novamart'@'%';

GRANT ALL PRIVILEGES ON auth_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON product_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON cart_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'novamart'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'novamart'@'%';

GRANT ALL PRIVILEGES ON zeno_auth.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_products.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_cart.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_orders.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_payments.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_inventory.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON zeno_notifications.* TO 'novamart'@'localhost';

GRANT ALL PRIVILEGES ON auth_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON product_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON cart_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON order_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON payment_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'novamart'@'localhost';
GRANT ALL PRIVILEGES ON notification_db.* TO 'novamart'@'localhost';

-- 5. Apply Privileges
FLUSH PRIVILEGES;
