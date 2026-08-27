# Nova Mart — Comprehensive API Reference & Integration Guide

The authoritative machine-readable contract is [`api-contract/openapi.yaml`](../api-contract/openapi.yaml). All client requests enter via the unified **API Gateway** on port `8080` at prefix `http://localhost:8080/api/v1`.

---

## 1. Microservice Endpoint Directory

| Microservice | Base Path | Port | Key Endpoints | Auth Level |
| --- | --- | --- | --- | --- |
| **Auth Service** | `/api/v1/auth`, `/api/v1/users` | `8081` | `POST /auth/register`, `POST /auth/login`, `GET /users/me`, `PUT /users/{id}/status`, `PUT /users/{id}/role` | Public / Shopper / Admin |
| **Product Service** | `/api/v1/products`, `/api/v1/categories`, `/api/v1/brands`, `/api/v1/reviews` | `8082` | `GET /products`, `POST /products/{id}/images`, `GET /products/{id}/reviews`, `POST /products/{id}/reviews` | Public / Admin / Shopper |
| **Cart Service** | `/api/v1/cart`, `/api/v1/wishlist` | `8083` | `GET /cart`, `POST /cart/items`, `GET /wishlist`, `POST /wishlist`, `POST /wishlist/{productId}/move-to-cart` | Shopper (JWT) |
| **Order Service** | `/api/v1/orders`, `/api/v1/coupons` | `8084` | `POST /orders` (Saga), `GET /orders/{id}`, `POST /coupons/validate`, `GET /coupons`, `POST /coupons` | Shopper / Admin |
| **Payment Service** | `/api/v1/payments` | `8085` | `POST /payments`, `POST /payments/{id}/refund`, `GET /payments/{id}` | Internal / Saga / Admin |
| **Inventory Service** | `/api/v1/inventory` | `8086` | `GET /inventory`, `POST /inventory/reserve`, `POST /inventory/commit`, `POST /inventory/release`, `PUT /inventory/{productId}` | Internal / Admin |
| **Notification Service** | `/api/v1/notifications` | `8087` | `GET /notifications`, `GET /notifications/unread-count`, `PUT /notifications/{id}/read`, `PUT /notifications/read-all` | Shopper / Admin |

---

## 2. Universal Response Envelope

All endpoints consistently return the structured response envelope:

### Success Response (`200 OK`, `201 Created`)
```json
{
  "success": true,
  "message": "Resource processed successfully",
  "data": { }
}
```

### Error Response (`400`, `401`, `403`, `404`, `409`, `422`, `500`)
```json
{
  "success": false,
  "message": "Only 2 units of this item are available",
  "errorCode": "INSUFFICIENT_STOCK",
  "timestamp": "2026-08-24T10:21:02.311Z",
  "path": "/api/v1/cart/items"
}
```

### Validation Error (`400 Bad Request`)
```json
{
  "success": false,
  "message": "Request validation failed",
  "errorCode": "VALIDATION_FAILED",
  "timestamp": "2026-08-24T09:40:12.882Z",
  "path": "/api/v1/auth/register",
  "fieldErrors": [
    { "field": "email", "message": "Must be a well-formed email address" },
    { "field": "password", "message": "Password must be at least 8 characters" }
  ]
}
```

---

## 3. New Feature Endpoint Specifications

### A. Customer Wishlist (`cart-service`)
- `GET /api/v1/wishlist` — Retrieve the current user's saved wishlist items with live price and stock status.
- `POST /api/v1/wishlist` — Add a product to wishlist (`{ "productId": "uuid" }`).
- `DELETE /api/v1/wishlist/{productId}` — Remove a product from wishlist.
- `POST /api/v1/wishlist/{productId}/move-to-cart` — Move item from wishlist to active shopping cart in one atomic operation.

### B. Product Reviews & Ratings (`product-service`)
- `GET /api/v1/products/{id}/reviews?page=0&size=10` — List customer reviews with verified purchaser badges.
- `GET /api/v1/products/{id}/reviews/summary` — Get average rating, total review count, and star breakdown (1★ through 5★).
- `POST /api/v1/products/{id}/reviews` — Submit a review (`{ "rating": 5, "title": "Great sound", "comment": "..." }`).
- `DELETE /api/v1/reviews/{id}` — Delete a review (author or Admin).

### C. Promotional Coupons & Discounts (`order-service`)
- `POST /api/v1/coupons/validate` — Validate a promo code against cart subtotal (`{ "code": "SAVE20", "cartSubtotal": 2000.00 }`).
- `GET /api/v1/coupons` — Admin list of all coupons and usage statistics.
- `POST /api/v1/coupons` — Admin create coupon (`PERCENTAGE`, `FIXED_AMOUNT`, or `FREE_SHIPPING`).
- `DELETE /api/v1/coupons/{id}` — Admin delete coupon.

### D. Notification Center & Read State (`notification-service`)
- `GET /api/v1/notifications?page=0&size=20` — List notifications for authenticated user.
- `GET /api/v1/notifications/unread-count` — Instant count of unread notifications for navigation badges.
- `PUT /api/v1/notifications/{id}/read` — Mark a single notification as read.
- `PUT /api/v1/notifications/read-all` — Mark all user notifications as read.

### E. User Role & Status Moderation (`auth-service`)
- `GET /api/v1/users?search=name&page=0&size=20` — Admin list and search users.
- `PUT /api/v1/users/{id}/status?enabled=true|false` — Admin enable or disable user account.
- `PUT /api/v1/users/{id}/role` — Admin promote or demote user roles (`{ "roles": ["USER", "ADMIN"] }`).
