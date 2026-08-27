# ZENO MART — Online Shopping Platform

An enterprise-grade, production-shaped online shopping platform built as **eight independent microservices behind a Spring Cloud API Gateway**, designed with an **API-First** methodology, featuring a modern **Next.js 15** storefront and back-office administrative dashboard.

> **Academic Submission Context** — Implements the requirement: *"Design a Microservices architecture using an API-First Approach for Online Shopping with Database-per-Service ownership and Distributed Saga Transactions."* Detailed defense and rationale are provided in [`docs/architecture.md`](docs/architecture.md) and [`docs/viva-preparation.md`](docs/viva-preparation.md).

---

## 1. System Architecture Overview

```text
                    ZENO MART
                        │
              ┌─────────┴─────────┐
              │                   │
            USER                ADMIN
              │                   │
       Customer Access       Full Admin Access
              │                   │
   ┌──────────┼──────────┐   ┌────┼───────────────┐
   │          │          │   │    │               │
Products     Cart      Orders Admin Products   Inventory
Wishlist    Profile    Review Dashboard        Coupons
Checkout    Notifications     Users / Roles    Orders
```

| Microservice | Port | Database | Primary Responsibilities |
| --- | --- | --- | --- |
| **API Gateway** | `8080` | None (Routing) | Reverse proxy, CORS, JWT token verification, header sanitization |
| **Auth Service** | `8081` | `auth_db` | Shopper & Admin registration, login, JWT rotation, address book, user role moderation |
| **Product Service** | `8082` | `product_db` | Catalogue, categories, brands, specifications, local image uploads, reviews & ratings |
| **Cart Service** | `8083` | `cart_db` | Persistent shopping carts, customer wishlist management, move-to-bag operations |
| **Order Service** | `8084` | `order_db` | Distributed Saga orchestrator, order tracking, printable invoices, coupon validation |
| **Payment Service** | `8085` | `payment_db` | Simulated payment gateway, authorizations, captures, refund processing |
| **Inventory Service** | `8086` | `inventory_db` | Real-time stock levels, pessimistic reservations, commits, releases, low-stock audit |
| **Notification Service** | `8087` | `notification_db` | Notification log, order confirmations, welcome emails, live unread tracking |

---

## 2. Key Capabilities & Implemented Features

1. **Database-per-Service Isolation (MySQL & H2)**:
   - Each service has exclusive ownership over its database (`auth_db`, `product_db`, `cart_db`, `order_db`, `payment_db`, `inventory_db`, `notification_db`).
   - Zero cross-database joins or cross-service SQL queries.
   - Independent Flyway database migrations per service.

2. **Distributed Saga Checkout Workflow**:
   - Automated 4-service transaction orchestration in `order-service`.
   - Pessimistic inventory reservations before payment.
   - Automatic compensating transactions (releasing reserved stock) on payment failures.

3. **Customer Wishlist System**:
   - One-click heart toggle on product cards and product details.
   - Dedicated `/wishlist` storefront page and `/account/wishlist` dashboard.
   - "Move to Bag" batch conversion from wishlist to cart.

4. **Product Reviews & 5-Star Ratings**:
   - Verified buyer badges, review submission modal with interactive star rating selector.
   - Real-time rating breakdown (5★ to 1★ distribution) and weighted averages.
   - Star rating filtering (`minRating`) on product catalogue search.

5. **Promotional Coupons & Discounts**:
   - Discount types: `PERCENTAGE`, `FIXED_AMOUNT`, and `FREE_SHIPPING`.
   - Real-time coupon validation at checkout (`POST /api/v1/coupons/validate`).
   - Back-office coupon management dashboard at `/admin/coupons`.

6. **Order Tracking, Printable Invoices & Reorder**:
   - Step-by-step visual order timeline with `OUT_FOR_DELIVERY` status support.
   - Printable GSTIN tax invoices with `@media print` layout at `/account/orders/[id]/invoice`.
   - One-click "Buy Again" reordering from past order history.

7. **Admin Back-Office Suite**:
   - Real-time analytics dashboard with revenue trends and order status breakdown charts.
   - Product catalogue management with local device image uploads (`PNG`/`JPG`/`WEBP`).
   - Live stock management with reorder thresholds and low-stock alerts.
   - User account moderation: account disable/enable and `USER` &harr; `ADMIN` role promotion.

8. **Notification Center**:
   - Live unread notification counter badge in navigation header.
   - User notification center at `/account/notifications` with individual and batch "Mark as Read".

---

## 3. Quick Start & Execution

### Option A — Zero-Install In-Memory Run (Recommended for Local Dev)

Start the microservices:
```powershell
# In root directory:
./start-local.ps1
```

Start the Next.js frontend:
```bash
cd frontend
npm install
npm run dev
```

- **Storefront**: [http://localhost:3000](http://localhost:3000)
- **Admin Dashboard**: [http://localhost:3000/admin](http://localhost:3000/admin)
- **API Gateway**: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)

### Option B — Production Docker with MySQL 8.0

```bash
docker compose -f docker-compose.mysql.yml up --build
```

---

## 4. Test Suite Verification

| Test Suite | Commands | Results | Status |
| --- | --- | --- | --- |
| **Backend Modules (10/10)** | `./mvnw clean test` | 287 tests (264 unit + 23 integration) | **100% Passed (0 Failures)** |
| **Frontend Unit Tests** | `npm test` (vitest) | 40 tests across 8 suites | **100% Passed (0 Failures)** |
| **TypeScript Typecheck** | `npx tsc --noEmit` | Strict mode check across 24+ routes | **0 Errors** |
| **OpenAPI 3.0.3 Contract** | Linting & Schema Check | `api-contract/openapi.yaml` | **0 Errors, Valid 3.0.3** |

---

## 5. Demo Accounts

| Role | Email | Password | Access Privileges |
| --- | --- | --- | --- |
| **Administrator** | `admin@novamart.dev` | `Admin@12345` | Full access to Admin Dashboard, Users, Products, Stock, Coupons, Orders |
| **Customer (Shopper)** | `demo@novamart.dev` | `Demo@12345` | Full customer access: Shopping, Wishlist, Reviews, Checkout, Order Tracking |
