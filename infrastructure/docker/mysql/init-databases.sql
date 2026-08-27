-- Creates one database per service inside a MySQL server.
--
-- ARCHITECTURAL NOTE:
-- Database-per-service pattern is strictly maintained. Each microservice
-- owns its own database schema, credentials, and connection URL.
--
-- Recommended MySQL Databases:
--   zeno_auth          (Auth Service)
--   zeno_products      (Product Catalog Service)
--   zeno_cart          (Cart & Wishlist Service)
--   zeno_orders        (Order Service & Saga)
--   zeno_payments      (Payment Service)
--   zeno_inventory     (Inventory Service)
--   zeno_notifications (Notification Service)

CREATE DATABASE IF NOT EXISTS zeno_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_products CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_cart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_orders CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_payments CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zeno_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Also create the standard fallback names for compatibility
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges to the novamart user
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

FLUSH PRIVILEGES;
