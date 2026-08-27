# Nova Mart — Academic Viva & Technical Defense Guide

This guide contains the most critical architectural, design, and implementation questions expected during an academic project review or technical viva examination, along with precise, high-scoring answers.

---

## 1. Core Architecture & Microservices Questions

### Q1: Why did you choose a Microservices architecture instead of a Monolith?
**Answer**:
"We chose Microservices because an e-commerce platform has fundamentally asymmetrical scaling requirements and decoupled business domains:
1. **Asymmetric Load**: The product catalog and reviews experience high read traffic (95%+ reads), whereas the checkout process is write-heavy and transactional. Separating `product-service` from `order-service` ensures browsing traffic never starves checkout connection pools.
2. **Independent Deployability**: Changes to promotions/coupons or payment gateway integrations can be deployed independently without redeploying or risking downtime for the entire catalogue.
3. **Fault Isolation / Blast Radius**: A failure in the `notification-service` does not block a customer from placing an order; the order is confirmed and the notification failure is logged gracefully."

### Q2: Why did you avoid heavy message brokers like Kafka or RabbitMQ?
**Answer**:
"For our deployment profile and synchronous checkout UX requirements:
1. The customer needs an immediate, deterministic response on their screen (e.g. 'Order Confirmed' vs 'Card Declined' vs 'Item Out of Stock').
2. An orchestration-based synchronous Saga over HTTP REST via the API Gateway allows direct error propagation and transactional compensations without requiring background message broker infrastructure."

---

## 2. Distributed Transactions & Data Consistency

### Q3: How do you handle distributed transactions across multiple services without Two-Phase Commit (2PC)?
**Answer**:
"We implemented an **Orchestration-based Distributed Saga Pattern** in `order-service`:
- The `order-service` acts as the Saga Orchestrator.
- It executes a sequence of local transactions across services:
  1. Reads cart items (`cart-service`).
  2. Snapshots current product prices and calculates coupon discounts (`product-service` & `order-service`).
  3. Pre-allocates inventory via a **stock reservation** (`inventory-service`).
  4. Creates order in `PENDING` state (`order_db`).
  5. Charges payment (`payment-service`).
- If payment succeeds: reservations are **committed**, the cart is **cleared**, the order status becomes `CONFIRMED`, and a notification is dispatched.
- If payment fails: the orchestrator initiates **compensating transactions**—releasing the reserved inventory and transitioning the order to `CANCELLED`."

### Q4: How do you prevent double-charging or duplicate orders if a user double-clicks 'Pay'?
**Answer**:
"We use **Idempotency Keys**:
- The client generates a unique `Idempotency-Key` header with each checkout request.
- The `order-service` stores this key. If a repeated request arrives with the identical key within the idempotency window, the server returns the existing order result rather than re-running the payment and creating duplicate records."

---

## 3. Database & Entity Design

### Q5: How do you enforce the 'Database-per-Service' pattern?
**Answer**:
"Each microservice has its own dedicated database (`auth_db`, `product_db`, `cart_db`, `order_db`, `payment_db`, `inventory_db`, `notification_db`):
1. No microservice has database credentials or direct network access to another service's database.
2. Cross-service data queries (such as displaying product names on an order invoice) are fulfilled either via **immutable snapshots at order time** or by calling the owning service's REST API through the Gateway.
3. Schemas are version-controlled using independent Flyway migrations per service."

### Q6: Why do you store monetary values in `DECIMAL(12,2)` / `BigDecimal` instead of `FLOAT` or `DOUBLE`?
**Answer**:
"Binary floating-point types (`float`/`double`) cannot represent base-10 fractions (like 0.01 or 0.10) precisely due to IEEE 754 rounding errors. Over hundreds of order lines and tax calculations, floating-point math leads to accumulated round-off discrepancies. `BigDecimal` in Java and `DECIMAL(12,2)` in MySQL guarantee exact decimal precision."

---

## 4. Security & Authentication

### Q7: How does authentication and Role-Based Access Control (RBAC) work?
**Answer**:
"1. **Stateless JWTs**: When a user logs in, `auth-service` generates a signed HMAC-SHA256 JWT containing `userId`, `email`, and an array of `roles` (`USER` and/or `ADMIN`).
2. **API Gateway Verification**: The Gateway validates the JWT signature and token expiry on incoming requests. It strips any user-submitted `X-User-*` headers to prevent spoofing and injects verified headers downstream (`X-User-Id`, `X-User-Roles`).
3. **Role Separation**:
   - `USER` accounts have full access to customer operations: product browsing, search, wishlist, cart, coupon application, checkout, order tracking, and reviews.
   - `ADMIN` accounts have access to back-office endpoints: dashboard analytics, product management, stock adjustments, coupon creation, and user status/role moderation."

---

## 5. Summary Table for Viva Examiners

| Feature | Implementation Mechanism | Justification / Engineering Value |
| --- | --- | --- |
| **Architecture** | 8 Spring Boot Microservices + Next.js Frontend | Clean separation of concerns and independent scalability |
| **Gateway** | Spring Cloud Gateway (Port 8080) | Single entry point, SSL termination, JWT authentication |
| **Databases** | MySQL 8.0 / H2 (Database-per-service) | Complete schema isolation without cross-database joins |
| **Distributed Saga** | Orchestrator in `order-service` | Consistent multi-service checkouts with automatic rollbacks |
| **Migrations** | Flyway Per Service | Deterministic schema evolution across environments |
| **Search & Filters** | Category, Brand, Price Range, Min Rating | Index-backed SQL queries with sub-50ms latency |
| **Promotions** | Percentage, Fixed Amount, Free Shipping | Real-time validation and order-level discount calculation |
| **Security** | HS256 JWT + Role-based filters | Stateless, tamper-proof customer & admin access control |
