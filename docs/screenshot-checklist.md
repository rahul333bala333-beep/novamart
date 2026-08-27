# Nova Mart — Academic Submission Screenshot Checklist

This document details the exact 19 screenshots required for the academic project report and viva presentation.

---

## Required Screenshots Table

| # | Screen / Artifact | URL / Location | User Role | Key Elements to Highlight in Screenshot |
| :-: | :--- | :--- | :--- | :--- |
| **1** | **Home Page** | `http://localhost:3000` | Public / Shopper | Hero banner, value propositions (free shipping, warranties), category navigation tiles, and featured curated products. |
| **2** | **Product Listing (Catalogue)** | `http://localhost:3000/products` | Public / Shopper | Search bar, category filters (Audio, Computing, Home), price slider, sorting options, and paginated product grid. |
| **3** | **Product Details** | `http://localhost:3000/products/[slug]` | Public / Shopper | Product gallery images, live price & compare-at price, stock availability badge, detailed specs, and "Add to Bag" button. |
| **4** | **Login Screen** | `http://localhost:3000/login` | Public | Clean email and password fields, validation cues, demo credentials hint, and "Sign In" button. |
| **5** | **Registration Screen** | `http://localhost:3000/register` | Public | First name, last name, email, phone, password fields with live client-side validation rules. |
| **6** | **Shopping Cart / Bag** | `http://localhost:3000/cart` | Shopper | Line item list with thumbnail image, quantity stepper, live item prices, order summary subtotal, delivery calculation, and checkout button. |
| **7** | **Checkout Page** | `http://localhost:3000/checkout` | Shopper | Saved shipping address selector, new address form, order summary with delivery fee calculation, and payment method selector. |
| **8** | **Payment Simulation Panel** | `http://localhost:3000/checkout` | Shopper | Payment method options, simulated notice panel, and the interactive checkbox: *"Demonstration: force this payment to be declined."* |
| **9** | **Order Success Confirmation** | `http://localhost:3000/orders/[id]` | Shopper | Success banner, Order Number (`NM-ORD-...`), ordered items list, shipping address, and simulated payment confirmation. |
| **10** | **Order History & Lifecycle** | `http://localhost:3000/account/orders` | Shopper | List of historical orders with status badges (`PENDING`, `CONFIRMED`, `CANCELLED`), order timestamps, and "Cancel Order" action. |
| **11** | **Admin Dashboard Overview** | `http://localhost:3000/admin` | Admin | Aggregate business metrics (Total Revenue, Orders, Customers, Low Stock alerts) and quick management links. |
| **12** | **Admin Product Management** | `http://localhost:3000/admin/products` | Admin | Complete product catalog table with SKU, name, category, price, stock status, and the **"Add Product" modal with the new local drag-and-drop Image Upload component**. |
| **13** | **Admin Inventory Management** | `http://localhost:3000/admin/inventory` | Admin | Stock table showing Total Quantity, Reserved Quantity, Available Quantity, Reorder Threshold, and manual stock adjustment modal. |
| **14** | **Interactive Swagger UI** | `http://localhost:8082/swagger-ui.html` | Developer / Reviewer | SpringDoc Swagger UI showing all endpoints including the new multipart `POST /api/v1/products/{id}/image`. |
| **15** | **OpenAPI 3.0.3 Contract** | `api-contract/openapi.yaml` / Redoc | Developer / Reviewer | Formatted OpenAPI specification showing schema definitions, response envelopes, and error taxonomies. |
| **16** | **API Gateway Health & Actuator** | `http://localhost:8080/actuator/health` | DevOps / Admin | JSON response showing `"status": "UP"` and aggregated service health probes. |
| **17** | **Microservices Architecture Diagram** | `docs/architecture.md` | Academic | System architecture diagram illustrating the 7 microservices, API Gateway, and Next.js client. |
| **18** | **Database Isolation Architecture** | `docs/database.md` | Academic | Visual diagram showing the 7 isolated PostgreSQL databases (`auth_db` through `notification_db`) with zero cross-database joins. |
| **19** | **Automated Test Results** | Terminal Output (`mvn verify` & `vitest`) | QA / Evaluator | Clean test execution output proving 100% passing tests (Backend: 264 unit + 23 integration; Frontend: 40 Vitest assertions). |

---

## Demonstration Credentials for Capturing Screenshots

| Role | Email | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Shopper** | `demo@novamart.dev` | `Demo@12345` | Storefront browsing, cart operations, checkout saga, order history |
| **Administrator** | `admin@novamart.dev` | `Admin@12345` | Admin dashboard, local product image upload, stock adjustments, notifications |
