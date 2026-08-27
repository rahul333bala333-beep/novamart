-- Creates one database per service inside a single PostgreSQL server.
--
-- ARCHITECTURAL NOTE, because this is the compromise most worth explaining.
--
-- The microservices rule that matters is that a service owns its data and no
-- other service may read or write it. Seven separate databases, seven separate
-- connection strings, no shared tables, no cross-service foreign keys, and no
-- joins across a boundary: all of that holds here exactly as it would with seven
-- separate servers.
--
-- What is shared is the PostgreSQL *process*. Running seven containers on a
-- laptop costs roughly 1.5GB of RAM before a single service starts, which puts
-- the project out of reach of the machines it is meant to be demonstrated on.
-- The trade is deliberate: it costs isolation of failure and of resources (one
-- server going down takes all seven databases with it) and buys the ability to
-- run the whole platform on a student laptop.
--
-- Production would give each service its own instance. Nothing in the
-- application code would change, because each service already knows only its own
-- DB_URL.

CREATE DATABASE auth_db;
CREATE DATABASE product_db;
CREATE DATABASE cart_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE inventory_db;
CREATE DATABASE notification_db;

-- The application user is the owner of each database and has no rights over any
-- other. It is not a superuser.
GRANT ALL PRIVILEGES ON DATABASE auth_db TO novamart;
GRANT ALL PRIVILEGES ON DATABASE product_db TO novamart;
GRANT ALL PRIVILEGES ON DATABASE cart_db TO novamart;
GRANT ALL PRIVILEGES ON DATABASE order_db TO novamart;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO novamart;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO novamart;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO novamart;
