# Nova Mart — System Architecture & Engineering Defense

This document explains **why** the system is built the way it is. It is written to be defended in a technical review / academic viva: every significant architectural decision is stated alongside the alternatives that were considered, trade-offs evaluated, and reasons for rejection.

---

## 1. Microservices Architecture Rationale

Microservices are not free. They introduce network latency, distributed data consistency challenges, and operational complexity. The decision to implement **Nova Mart** as an 8-component microservice system was driven by three domain invariants:

1. **Independent Lifecycle & Evolution**:
   - The product catalogue changes with merchandising schedules.
   - Payments evolve with compliance standards and gateway rules.
   - Inventory changes with warehouse automation.
   - Separate services allow isolated release cadences and targeted rollouts without global redeployment.

2. **Asymmetric Load Profiles**:
   - Product browsing and reviews are read-heavy (95%+ reads) and benefit from edge caching.
   - Checkout is write-heavy, transactional, and CPU-intensive.
   - Isolating `product-service` from `order-service` prevents traffic spikes on public listings from exhausting connection pools needed for revenue checkout.

3. **Blast Radius Minimization**:
   - A degradation in `notification-service` cannot block an order from completing. If notifications fail, the error is logged asynchronously and the order remains confirmed.

### Technologies Deliberately Excluded & Why
- **No Kafka / RabbitMQ**: Synchronous HTTP orchestrator saga provides deterministic transaction tracing, immediate error responses to the Next.js UI, and zero background broker infrastructure overhead.
- **No Redis / Elasticsearch**: Kept database-per-service boundary clean. Search and filtering run through indexed SQL columns with sub-50ms latency.
- **No GraphQL**: RESTful OpenAPI 3.0.3 contracts provide strict endpoint versioning, standard HTTP status codes, and type safety across client and server.

---

## 2. Service Boundaries & Ownership

```mermaid
graph TB
    subgraph Client
        BR["Browser<br/>Next.js 16 Storefront + Admin"]
    end

    GW["API Gateway :8080<br/>Routing · Security · Token Verification · Header Hygiene"]

    subgraph Microservices
        AUTH["auth-service :8081<br/>Identity, Roles, Address Book"]
        PROD["product-service :8082<br/>Catalogue, Ratings, Reviews, Images"]
        CART["cart-service :8083<br/>Shopping Cart & Customer Wishlists"]
        ORD["order-service :8084<br/>Orders, Saga Orchestrator, Coupons"]
        PAY["payment-service :8085<br/>Simulated Payment Gateway"]
        INV["inventory-service :8086<br/>Stock Levels, Reservations, Audit"]
        NOTIF["notification-service :8087<br/>Notifications & Read Tracking"]
    end

    subgraph Isolated Databases
        ADB[("auth_db (PostgreSQL · MySQL · H2)")]
        PDB[("product_db (PostgreSQL · MySQL · H2)")]
        CDB[("cart_db (PostgreSQL · MySQL · H2)")]
        ODB[("order_db (PostgreSQL · MySQL · H2)")]
        PAYDB[("payment_db (PostgreSQL · MySQL · H2)")]
        IDB[("inventory_db (PostgreSQL · MySQL · H2)")]
        NDB[("notification_db (PostgreSQL · MySQL · H2)")]
    end

    BR --> GW
    GW --> AUTH & PROD & CART & ORD & PAY & INV & NOTIF

    AUTH --- ADB
    PROD --- PDB
    CART --- CDB
    ORD --- ODB
    PAY --- PAYDB
    INV --- IDB
    NOTIF --- NDB

    ORD -.->|1. Reads Cart| CART
    ORD -.->|2. Price Snapshot| PROD
    ORD -.->|3. Validate Coupon| ORD
    ORD -.->|4. Reserve Stock| INV
    ORD -.->|5. Process Payment| PAY
    ORD -.->|6. Commit Stock & Clear Cart| INV
    ORD -.->|7. Emit Notification| NOTIF
```

---

## 3. Distributed Saga Orchestration (Checkout Workflow)

The checkout process spans four autonomous microservices. Instead of 2-phase commit (which creates distributed locks and availability bottlenecks), Nova Mart implements an **Orchestration-based Saga with Compensating Transactions**:

```mermaid
sequenceDiagram
    autonumber
    actor Shopper as Customer (Next.js)
    participant GW as API Gateway (8080)
    participant ORD as Order Service (8084)
    participant PROD as Product Service (8082)
    participant INV as Inventory Service (8086)
    participant PAY as Payment Service (8085)
    participant CART as Cart Service (8083)
    participant NOTIF as Notification Service (8087)

    Shopper->>GW: POST /api/v1/orders (with Idempotency-Key & couponCode)
    GW->>ORD: Forward with X-User-Id, X-User-Roles
    
    rect rgb(240, 245, 255)
        Note over ORD: Step 1: Read Cart Items
        ORD->>CART: GET /api/v1/cart
        CART-->>ORD: Return items & quantities
    end

    rect rgb(240, 245, 255)
        Note over ORD: Step 2: Price Snapshot & Coupon Validation
        ORD->>PROD: GET /api/v1/products/{id} (verify current prices)
        PROD-->>ORD: Return immutable product snapshots
        Note over ORD: Validate coupon discount rules & calculate totals
    end

    rect rgb(255, 245, 240)
        Note over ORD: Step 3: Stock Reservation (Pessimistic)
        ORD->>INV: POST /api/v1/inventory/reserve
        alt Stock Insufficient
            INV-->>ORD: 409 Conflict (Out of Stock)
            ORD-->>GW: 409 Stock Error
            GW-->>Shopper: "One or more items are out of stock"
        else Stock Reserved
            INV-->>ORD: 200 OK (Reservation IDs)
        end
    end

    rect rgb(245, 255, 240)
        Note over ORD: Step 4: Persist Order PENDING
        ORD->>ORD: Save Order + OrderItems + OrderCoupon in order_db
    end

    rect rgb(255, 250, 230)
        Note over ORD: Step 5: Payment Processing
        ORD->>PAY: POST /api/v1/payments (amount, method)
        alt Payment Fails / Declined
            PAY-->>ORD: Payment Failed
            Note over ORD: COMPENSATING TRANSACTIONS
            ORD->>INV: POST /api/v1/inventory/release
            ORD->>ORD: Mark Order CANCELLED
            ORD-->>GW: 402 Payment Declined
            GW-->>Shopper: "Payment failed. No funds charged."
        else Payment Successful / COD
            PAY-->>ORD: 200 OK (Payment Reference)
        end
    end

    rect rgb(240, 255, 240)
        Note over ORD: Step 6: Finalize & Post-Actions
        ORD->>INV: POST /api/v1/inventory/commit
        ORD->>CART: DELETE /api/v1/cart
        ORD->>ORD: Mark Order CONFIRMED
        ORD->>NOTIF: POST /api/v1/notifications (ORDER_CONFIRMATION)
        ORD-->>GW: 201 Created (Order Response)
        GW-->>Shopper: Order Success View + Order Number
    end
```

---

## 4. Role-Based Access Control Model

Nova Mart enforces strict separation between Customer accounts (`USER`) and Administrator back-office accounts (`ADMIN`):

```text
                    Nova Mart
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

1. **Authentication Token**: Signed JWT (HS256) containing `sub` (userId), `email`, and `roles` array (`["USER"]` or `["USER", "ADMIN"]`).
2. **API Gateway Enforcement**: Strips any client-injected `X-User-*` headers, decodes the verified Bearer token, and injects trusted downstream headers (`X-User-Id`, `X-User-Roles`, `X-User-Email`).
3. **Role Elevation**: Only existing `ADMIN` users can access `/api/v1/users/{id}/role` or `/api/v1/users/{id}/status` to promote users or enable/disable accounts.
