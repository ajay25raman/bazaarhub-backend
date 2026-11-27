# BazaarHub — Stage 6: OpenAPI 3 / Swagger Documentation Audit

**Date:** 2026-11-18  
**Auditor:** Senior Spring Boot Security & Quality Engineer  
**Target:** BazaarHub Enterprise Multi-Vendor Backend (Java 21 / Spring Boot 4.1.2)  
**Remediation Task:** REM-DOC-01 — OpenAPI 3 / Swagger Documentation Standardization  
**Audit Scope:** 59 REST Controllers, 286 REST Endpoints  

---

## 1. Executive Summary & Baseline Metrics

An exhaustive, read-only audit of all 59 REST controllers and 286 REST endpoints in `com.bazaarhub.backend` was conducted to baseline OpenAPI 3 and Swagger documentation compliance.

| Metric Category | Audited Baseline | Target Compliance | Gaps Identified |
|:---|:---:|:---:|:---|
| **Total REST Controllers** | 59 | 59 | 1 controller missing `@Tag` (`HealthController`) |
| **Total REST Endpoints** | 286 | 286 | 1 endpoint missing `@Operation` (`HealthController.health`) |
| **Endpoints with `@Operation`** | 285 / 286 (99.6%) | 286 / 286 (100%) | 1 missing (`GET /api/v1/health`) |
| **Endpoints with `@ApiResponse`** | 156 / 286 (54.5%) | 286 / 286 (100%) | **130 endpoints** completely lack `@ApiResponse` annotations |
| **Endpoints with Error Codes Documented (4xx/5xx)** | 118 / 286 (41.3%) | 286 / 286 (100%) | **168 endpoints** lack comprehensive error responses (400, 401, 403, 404, 409, 422) |
| **Protected Endpoints with `@SecurityRequirement`** | 233 / 253 (92.1%) | 253 / 253 (100%) | **20 protected endpoints** across AI and Analytics missing `@SecurityRequirement` |
| **Public Endpoints without Security Requirement** | 33 / 33 (100%) | 33 / 33 (100%) | Accurately public (Auth public, Catalog public, Search/Discovery, Webhooks, Public AI) |
| **`@AuthenticationPrincipal` Hidden Parameters** | Partially standard | 100% hidden | Inconsistent use of `@Parameter(hidden = true)` exposing internal user objects |

---

## 2. Module Summary Breakdown

| Module Domain | Controllers | Endpoints | Fully Documented APIs | APIs Missing `@ApiResponse` | Missing Security |
|:---|:---:|:---:|:---:|:---:|:---:|
| **Ai** | 4 | 10 | 0 | 10 | 7 |
| **Analytics** | 3 | 14 | 0 | 14 | 13 |
| **Cart** | 2 | 9 | 9 | 0 | 0 |
| **Catalog** | 9 | 44 | 44 | 0 | 0 |
| **Customer** | 2 | 14 | 14 | 0 | 0 |
| **Delivery** | 3 | 29 | 0 | 29 | 0 |
| **Identity** | 1 | 7 | 7 | 0 | 5 |
| **Inventory** | 4 | 21 | 21 | 0 | 0 |
| **Notification** | 3 | 18 | 0 | 18 | 0 |
| **Order** | 3 | 15 | 15 | 0 | 0 |
| **Payment** | 3 | 14 | 14 | 0 | 1 |
| **Review** | 4 | 12 | 0 | 12 | 0 |
| **Search** | 2 | 6 | 6 | 0 | 0 |
| **Seller** | 5 | 19 | 19 | 0 | 0 |
| **Shared** | 1 | 1 | 0 | 1 | 0 |
| **Shipping** | 5 | 16 | 0 | 16 | 1 |
| **Support** | 4 | 30 | 0 | 30 | 0 |
| **Wishlist** | 1 | 7 | 7 | 0 | 0 |

---

## 3. High-Priority Gaps Identified

### GAP-01: HealthController Completely Undocumented
- File: `src/main/java/com/bazaarhub/backend/shared/presentation/rest/health/HealthController.java`
- Endpoint: `GET /api/v1/health`
- Missing `@Tag`, missing `@Operation`, missing `@ApiResponse`.

### GAP-02: AI & Analytics Protected Endpoints Missing Security Scheme
- Files:
  - `AdminAiController.java` (3 endpoints) - Admin only
  - `CustomerAiAssistantController.java` (3 endpoints) - Customer only
  - `SellerAiCatalogController.java` (1 endpoint) - Seller only
  - `AdminAnalyticsController.java` (9 endpoints) - Admin only
  - `SellerAnalyticsController.java` (4 endpoints) - Seller only
- These 20 endpoints require authenticated JWT tokens (`ROLE_ADMIN`, `ROLE_CUSTOMER`, `ROLE_SELLER`), but omit `@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)` at the class level.

### GAP-03: 27 Controllers with 130 Endpoints Missing All `@ApiResponse` Annotations
The following 27 controllers define `@Operation` but declare zero `@ApiResponse` or `@ApiResponses`:
1. `AdminAiController` (3 endpoints)
2. `CustomerAiAssistantController` (3 endpoints)
3. `PublicAiController` (3 endpoints)
4. `SellerAiCatalogController` (1 endpoint)
5. `AdminAnalyticsController` (9 endpoints)
6. `PublicAnalyticsEventController` (1 endpoint)
7. `SellerAnalyticsController` (4 endpoints)
8. `AdminDeliveryController` (11 endpoints)
9. `CustomerDeliveryTrackingController` (3 endpoints)
10. `DeliveryBoyController` (15 endpoints)
11. `AdminNotificationController` (6 endpoints)
12. `CustomerNotificationController` (6 endpoints)
13. `SellerNotificationController` (6 endpoints)
14. `AdminReviewController` (4 endpoints)
15. `CustomerReviewController` (5 endpoints)
16. `PublicReviewController` (2 endpoints)
17. `SellerReviewController` (1 endpoint)
18. `HealthController` (1 endpoint)
19. `AdminShipmentController` (5 endpoints)
20. `CustomerShipmentController` (2 endpoints)
21. `InternalShipmentController` (5 endpoints)
22. `SellerShipmentController` (2 endpoints)
23. `ShipmentTrackingController` (2 endpoints)
24. `AdminSupportController` (9 endpoints)
25. `CustomerSupportController` (7 endpoints)
26. `DeliverySupportController` (7 endpoints)
27. `SellerSupportController` (7 endpoints)

### GAP-04: Incomplete Response Codes on Existing Controllers
In the remaining 32 controllers, many endpoints only declare HTTP 200 or 201 success responses, neglecting standard error codes handled by `GlobalRestExceptionHandler`:
- `400 Bad Request`: Validation failure (`MethodArgumentNotValidException`, invalid query params, illegal states)
- `401 Unauthorized`: Missing or invalid Bearer JWT
- `403 Forbidden`: Insufficient role authority or BOLA tenant violation
- `404 Not Found`: `ResourceNotFoundException` (order, product, customer, seller, ticket, delivery)
- `409 Conflict`: `UserAlreadyExistsException`, unique constraint, concurrent modification
- `422 Unprocessable Content`: `BusinessRuleViolationException` (e.g., invalid seller state transitions, insufficient stock, invalid refunds)

### GAP-05: Parameter Documentation & Hidden Authentication Principals
- Any method accepting `@AuthenticationPrincipal UserPrincipal principal` must be annotated with `@Parameter(hidden = true)` to prevent Swagger UI from rendering an unnecessary request body/query schema for Spring Security authentication details.
- Query and path parameters (`@PathVariable`, `@RequestParam`, `@PageableDefault`) must be annotated with descriptive `@Parameter` attributes.

---

## 4. Comprehensive Controller & Endpoint Inventory

### Module: `ai` (4 Controllers)

#### AdminAiController
- **File:** `src/main/java/com/bazaarhub/backend/ai/presentation/rest/AdminAiController.java`
- **Base Path:** `/api/v1/admin/ai`
- **Class Tag:** `Admin AI Management`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/status` | Check AI operational health and provider settings | `NONE` | DEFICIENT |
| `GetMapping` | `/usage-statistics` | Retrieve AI token usage and estimated cost analytics | `NONE` | DEFICIENT |
| `PostMapping` | `/generate-description` | Admin catalog description generator | `NONE` | DEFICIENT |

#### CustomerAiAssistantController
- **File:** `src/main/java/com/bazaarhub/backend/ai/presentation/rest/CustomerAiAssistantController.java`
- **Base Path:** `/api/v1/customer/ai`
- **Class Tag:** `Customer AI Assistant`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/chat` | Chat with BazaarHub AI Shopping & Support Assistant | `NONE` | DEFICIENT |
| `GetMapping` | `/conversations` | List customer's AI conversation history | `NONE` | DEFICIENT |
| `GetMapping` | `/conversations/{id}/messages` | Retrieve messages in a conversation session | `NONE` | DEFICIENT |

#### PublicAiController
- **File:** `src/main/java/com/bazaarhub/backend/ai/presentation/rest/PublicAiController.java`
- **Base Path:** `/api/v1/public/ai`
- **Class Tag:** `Public AI Services`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/chat` | Guest inquiry chat with BazaarHub AI Assistant | `NONE` | PUBLIC |
| `GetMapping` | `/recommendations` | AI-powered product recommendations | `NONE` | PUBLIC |
| `GetMapping` | `/products/{productId}/review-summary` | AI product review sentiment synthesis | `NONE` | PUBLIC |

#### SellerAiCatalogController
- **File:** `src/main/java/com/bazaarhub/backend/ai/presentation/rest/SellerAiCatalogController.java`
- **Base Path:** `/api/v1/seller/ai`
- **Class Tag:** `Seller AI Catalog Tools`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (1):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/generate-description` | Generate AI product titles, descriptions, and SEO tags | `NONE` | DEFICIENT |

### Module: `analytics` (3 Controllers)

#### AdminAnalyticsController
- **File:** `src/main/java/com/bazaarhub/backend/analytics/presentation/rest/AdminAnalyticsController.java`
- **Base Path:** `/api/v1/admin/analytics`
- **Class Tag:** `Admin Analytics & Reporting`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (9):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/overview` | Platform-level high-level executive overview | `NONE` | DEFICIENT |
| `GetMapping` | `/sales` | Sales metrics and daily revenue time series | `NONE` | DEFICIENT |
| `GetMapping` | `/orders` | Order fulfillment and volume breakdown | `NONE` | DEFICIENT |
| `GetMapping` | `/products` | Catalog product performance and inventory health | `NONE` | DEFICIENT |
| `GetMapping` | `/categories` | Category sales performance | `NONE` | DEFICIENT |
| `GetMapping` | `/sellers` | Top performing sellers leaderboard | `NONE` | DEFICIENT |
| `GetMapping` | `/customers` | Customer cohort metrics and retention | `NONE` | DEFICIENT |
| `GetMapping` | `/delivery` | Logistics and shipping performance | `NONE` | DEFICIENT |
| `GetMapping` | `/support` | Customer support workload and resolution efficiency | `NONE` | DEFICIENT |

#### PublicAnalyticsEventController
- **File:** `src/main/java/com/bazaarhub/backend/analytics/presentation/rest/PublicAnalyticsEventController.java`
- **Base Path:** `/api/v1/public/analytics`
- **Class Tag:** `Analytics Event Ingestion`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (1):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/events` | Track analytics event | `NONE` | PUBLIC |

#### SellerAnalyticsController
- **File:** `src/main/java/com/bazaarhub/backend/analytics/presentation/rest/SellerAnalyticsController.java`
- **Base Path:** `/api/v1/seller/analytics`
- **Class Tag:** `Seller Analytics`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (4):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/overview` | Seller dashboard executive overview | `NONE` | DEFICIENT |
| `GetMapping` | `/sales` | Seller sales performance and daily revenue | `NONE` | DEFICIENT |
| `GetMapping` | `/products` | Seller top performing products | `NONE` | DEFICIENT |
| `GetMapping` | `/orders` | Seller order status breakdown and fulfillment rate | `NONE` | DEFICIENT |

### Module: `cart` (2 Controllers)

#### CustomerCartController
- **File:** `src/main/java/com/bazaarhub/backend/cart/presentation/rest/CustomerCartController.java`
- **Base Path:** `/api/v1/cart`
- **Class Tag:** `Customer Cart Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (6):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Get current customer's cart | `200, 401` | COMPLIANT |
| `PostMapping` | `/items` | Add item to cart | `201, 400, 401, 404, 422` | COMPLIANT |
| `PutMapping` | `/items/{itemId}` | Update cart item quantity | `200, 400, 401, 404, 422` | COMPLIANT |
| `DeleteMapping` | `/items/{itemId}` | Remove item from cart | `200, 401, 403, 404` | COMPLIANT |
| `DeleteMapping` | `/` | Clear all items from cart | `200, 401` | COMPLIANT |
| `GetMapping` | `/summary` | Get cart summary | `200, 401` | COMPLIANT |

#### CustomerCheckoutController
- **File:** `src/main/java/com/bazaarhub/backend/cart/presentation/rest/CustomerCheckoutController.java`
- **Base Path:** `/api/v1/checkout`
- **Class Tag:** `Customer Checkout Preparation`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/summary` | Get checkout summary | `200, 401, 403, 404` | COMPLIANT |
| `PostMapping` | `/validate` | Validate checkout readiness | `200, 400, 401, 403, 404` | COMPLIANT |
| `PostMapping` | `/address` | Select delivery address for checkout | `200, 400, 401, 403, 404` | COMPLIANT |

### Module: `catalog` (9 Controllers)

#### AdminBrandController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/AdminBrandController.java`
- **Base Path:** `/api/v1/admin/brands`
- **Class Tag:** `Admin Brand Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Create brand | `201, 400, 403, 409` | COMPLIANT |
| `GetMapping` | `/` | List brands | `200` | COMPLIANT |
| `GetMapping` | `/{id}` | Get brand by ID | `200, 404` | COMPLIANT |
| `PutMapping` | `/{id}` | Update brand | `200, 400, 404, 409` | COMPLIANT |
| `PatchMapping` | `/{id}/status` | Activate or deactivate brand | `200, 404, 422` | COMPLIANT |

#### AdminCategoryController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/AdminCategoryController.java`
- **Base Path:** `/api/v1/admin/categories`
- **Class Tag:** `Admin Category Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Create category | `201, 400, 403, 409` | COMPLIANT |
| `GetMapping` | `/` | List categories | `200` | COMPLIANT |
| `GetMapping` | `/{id}` | Get category by ID | `200, 404` | COMPLIANT |
| `PutMapping` | `/{id}` | Update category | `200, 400, 404, 409` | COMPLIANT |
| `PatchMapping` | `/{id}/status` | Activate or deactivate category | `200, 404, 422` | COMPLIANT |

#### AdminProductController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/AdminProductController.java`
- **Base Path:** `/api/v1/admin/products`
- **Class Tag:** `Admin Product Moderation`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List marketplace products for moderation | `200, 403` | COMPLIANT |
| `GetMapping` | `/{productId}` | Get product review details | `200, 404` | COMPLIANT |
| `PostMapping` | `/{productId}/approve` | Approve product | `200, 404` | COMPLIANT |
| `PostMapping` | `/{productId}/reject` | Reject product | `200, 400, 404` | COMPLIANT |
| `PatchMapping` | `/{productId}/status` | Update product status as admin | `200, 404` | COMPLIANT |

#### BrandController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/BrandController.java`
- **Base Path:** `/api/v1/brands`
- **Class Tag:** `Catalog Brands`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (4):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List active brands | `200` | PUBLIC |
| `GetMapping` | `/all` | Get all active brands | `200` | PUBLIC |
| `GetMapping` | `/{id}` | Get active brand by ID | `200, 404` | PUBLIC |
| `GetMapping` | `/slug/{slug}` | Get active brand by slug | `200, 404` | PUBLIC |

#### CategoryController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/CategoryController.java`
- **Base Path:** `/api/v1/categories`
- **Class Tag:** `Catalog Categories`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List active categories | `200` | PUBLIC |
| `GetMapping` | `/tree` | Get category hierarchy | `200` | PUBLIC |
| `GetMapping` | `/root` | Get root categories | `200` | PUBLIC |
| `GetMapping` | `/{id}` | Get active category by ID | `200, 404` | PUBLIC |
| `GetMapping` | `/slug/{slug}` | Get active category by slug | `200, 404` | PUBLIC |

#### ProductController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/ProductController.java`
- **Base Path:** `/api/v1/products`
- **Class Tag:** `Product Catalog`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Search public catalog products | `200` | DEFICIENT |
| `GetMapping` | `/{productId}` | Get product details by ID | `200, 404` | DEFICIENT |
| `GetMapping` | `/slug/{slug}` | Get product details by slug | `200, 404` | DEFICIENT |

#### SellerProductController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/SellerProductController.java`
- **Base Path:** `/api/v1/seller/products`
- **Class Tag:** `Seller Product Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (6):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Create product | `201, 400, 403, 409` | COMPLIANT |
| `GetMapping` | `/` | List seller's products | `200` | COMPLIANT |
| `GetMapping` | `/{productId}` | Get seller product by ID | `200, 403, 404` | COMPLIANT |
| `PutMapping` | `/{productId}` | Update product details | `200, 400, 403, 404, 409` | COMPLIANT |
| `PatchMapping` | `/{productId}/status` | Update product status | `200, 400, 403, 404` | COMPLIANT |
| `DeleteMapping` | `/{productId}` | Archive product | `200, 403, 404` | COMPLIANT |

#### SellerProductImageController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/SellerProductImageController.java`
- **Base Path:** `/api/v1/seller/products/{productId}/images`
- **Class Tag:** `Seller Product Images`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Add image to product | `201, 400, 403, 404` | COMPLIANT |
| `GetMapping` | `/` | List product images | `200, 403, 404` | COMPLIANT |
| `PatchMapping` | `/{imageId}/primary` | Set primary hero image | `200, 403, 404` | COMPLIANT |
| `PatchMapping` | `/{imageId}/order` | Update image display order | `200, 403, 404` | COMPLIANT |
| `DeleteMapping` | `/{imageId}` | Delete product image | `200, 403, 404` | COMPLIANT |

#### SellerProductVariantController
- **File:** `src/main/java/com/bazaarhub/backend/catalog/presentation/rest/SellerProductVariantController.java`
- **Base Path:** `/api/v1/seller/products/{productId}/variants`
- **Class Tag:** `Seller Product Variants`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (6):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Create product variant | `201, 400, 403, 404, 409` | COMPLIANT |
| `GetMapping` | `/` | List product variants | `200, 403, 404` | COMPLIANT |
| `GetMapping` | `/{variantId}` | Get variant by ID | `200, 403, 404` | COMPLIANT |
| `PutMapping` | `/{variantId}` | Update product variant | `200, 400, 403, 404, 409` | COMPLIANT |
| `PatchMapping` | `/{variantId}/status` | Toggle variant status | `200, 403, 404` | COMPLIANT |
| `DeleteMapping` | `/{variantId}` | Delete product variant | `200, 403, 404` | COMPLIANT |

### Module: `customer` (2 Controllers)

#### CustomerAddressController
- **File:** `src/main/java/com/bazaarhub/backend/customer/presentation/rest/CustomerAddressController.java`
- **Base Path:** `/api/v1/customer/addresses`
- **Class Tag:** `Customer Address Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Add delivery address | `201, 400, 401` | COMPLIANT |
| `GetMapping` | `/` | Get all addresses | `200, 401` | COMPLIANT |
| `GetMapping` | `/default` | Get default delivery address | `200, 401, 404` | COMPLIANT |
| `GetMapping` | `/{id}` | Get address by ID | `200, 401, 403, 404` | COMPLIANT |
| `PutMapping` | `/{id}` | Update delivery address | `200, 400, 401, 403, 404` | COMPLIANT |
| `PatchMapping` | `/{id}/default` | Set address as default | `200, 401, 403, 404` | COMPLIANT |
| `DeleteMapping` | `/{id}` | Delete delivery address | `200, 401, 403, 404, 409` | COMPLIANT |

#### CustomerProfileController
- **File:** `src/main/java/com/bazaarhub/backend/customer/presentation/rest/CustomerProfileController.java`
- **Base Path:** `/api/v1/customer/profile`
- **Class Tag:** `Customer Profile Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Get current customer profile | `200, 401, 404` | COMPLIANT |
| `GetMapping` | `/account` | Get complete customer account profile | `200, 401` | COMPLIANT |
| `PostMapping` | `/` | Create customer profile | `201, 400, 401, 409` | COMPLIANT |
| `PutMapping` | `/` | Update customer profile | `200, 400, 401` | COMPLIANT |
| `PatchMapping` | `/dob` | Update customer date of birth | `200, 400, 401` | COMPLIANT |
| `PatchMapping` | `/gender` | Update customer gender | `200, 400, 401` | COMPLIANT |
| `PatchMapping` | `/image` | Update customer profile image URL | `200, 400, 401` | COMPLIANT |

### Module: `delivery` (3 Controllers)

#### AdminDeliveryController
- **File:** `src/main/java/com/bazaarhub/backend/delivery/presentation/rest/AdminDeliveryController.java`
- **Base Path:** `/api/v1/admin/delivery`
- **Class Tag:** `Admin Delivery Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (11):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/boys` | List delivery partners | `NONE` | DEFICIENT |
| `GetMapping` | `/boys/{id}` | Get delivery partner details | `NONE` | DEFICIENT |
| `PatchMapping` | `/boys/{id}/verify` | Verify delivery partner KYC | `NONE` | DEFICIENT |
| `PatchMapping` | `/boys/{id}/status` | Update delivery partner status | `NONE` | DEFICIENT |
| `PostMapping` | `/shipments` | Create shipment for seller sub-order | `NONE` | DEFICIENT |
| `GetMapping` | `/shipments` | List shipments | `NONE` | DEFICIENT |
| `GetMapping` | `/shipments/{id}` | Get shipment details | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments` | Assign delivery partner to shipment | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments/{id}/reassign` | Reassign delivery partner | `NONE` | DEFICIENT |
| `GetMapping` | `/assignments` | List all assignments | `NONE` | DEFICIENT |
| `GetMapping` | `/tracking/{shipmentId}` | Get shipment tracking history | `NONE` | DEFICIENT |

#### CustomerDeliveryTrackingController
- **File:** `src/main/java/com/bazaarhub/backend/delivery/presentation/rest/CustomerDeliveryTrackingController.java`
- **Base Path:** `/api/v1/delivery/tracking`
- **Class Tag:** `Customer Delivery Tracking`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/{shipmentId}` | Get latest live location of shipment | `NONE` | DEFICIENT |
| `GetMapping` | `/{shipmentId}/estimate` | Get estimated time of arrival | `NONE` | DEFICIENT |
| `GetMapping` | `/shipments/my` | Get my package shipments | `NONE` | DEFICIENT |

#### DeliveryBoyController
- **File:** `src/main/java/com/bazaarhub/backend/delivery/presentation/rest/DeliveryBoyController.java`
- **Base Path:** `/api/v1/delivery`
- **Class Tag:** `Delivery Partner Operations`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (15):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/onboard` | Onboard as delivery partner | `NONE` | DEFICIENT |
| `GetMapping` | `/profile` | Get delivery partner profile | `NONE` | DEFICIENT |
| `PutMapping` | `/profile` | Update delivery partner profile | `NONE` | DEFICIENT |
| `PatchMapping` | `/availability` | Toggle availability status | `NONE` | DEFICIENT |
| `GetMapping` | `/assignments` | List assigned deliveries | `NONE` | DEFICIENT |
| `GetMapping` | `/assignments/{id}` | Get delivery assignment details | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments/{id}/accept` | Accept delivery assignment | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments/{id}/reject` | Reject delivery assignment | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments/{id}/pickup` | Confirm parcel pickup | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments/{id}/out-for-delivery` | Mark out for delivery | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments/{id}/complete` | Complete delivery with OTP | `NONE` | DEFICIENT |
| `PostMapping` | `/assignments/{id}/fail` | Report delivery failure | `NONE` | DEFICIENT |
| `PostMapping` | `/tracking/location` | Update live GPS location | `NONE` | DEFICIENT |
| `GetMapping` | `/route-estimate/{shipmentId}` | Get route distance and duration estimate | `NONE` | DEFICIENT |
| `PostMapping` | `/proof/upload` | Upload delivery proof image | `NONE` | DEFICIENT |

### Module: `identity` (1 Controllers)

#### AuthController
- **File:** `src/main/java/com/bazaarhub/backend/identity/presentation/rest/AuthController.java`
- **Base Path:** `/api/v1/auth`
- **Class Tag:** `Authentication`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/register` | Register new customer account | `201, 400, 409` | DEFICIENT |
| `PostMapping` | `/login` | Authenticate user credentials | `200, 401, 403` | DEFICIENT |
| `GetMapping` | `/me` | Get current authenticated user profile | `200, 401, 404` | COMPLIANT |
| `PostMapping` | `/logout` | Logout user and deactivate device session | `200, 401` | COMPLIANT |
| `PostMapping` | `/refresh` | Refresh JWT access token | `501` | DEFICIENT |
| `PostMapping` | `/forgot-password` | Initiate password recovery | `501` | DEFICIENT |
| `PostMapping` | `/reset-password` | Complete password reset | `501` | DEFICIENT |

### Module: `inventory` (4 Controllers)

#### AdminInventoryController
- **File:** `src/main/java/com/bazaarhub/backend/inventory/presentation/rest/AdminInventoryController.java`
- **Base Path:** `/api/v1/admin/inventory`
- **Class Tag:** `Admin Inventory`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (8):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List all platform inventory | `200, 403` | COMPLIANT |
| `GetMapping` | `/low-stock` | List low-stock inventory | `200` | COMPLIANT |
| `GetMapping` | `/out-of-stock` | List out-of-stock inventory | `200` | COMPLIANT |
| `GetMapping` | `/{variantId}` | Get inventory by variant ID | `200, 404` | COMPLIANT |
| `PostMapping` | `/{variantId}/adjustment` | Authoritative stock adjustment | `200, 400, 404, 422` | COMPLIANT |
| `PatchMapping` | `/{variantId}/safety-stock` | Update safety stock threshold | `200, 400, 404` | COMPLIANT |
| `GetMapping` | `/{variantId}/movements` | View variant movement history | `200, 404` | COMPLIANT |
| `GetMapping` | `/reservations` | List stock reservations | `200` | COMPLIANT |

#### InternalStockReservationController
- **File:** `src/main/java/com/bazaarhub/backend/inventory/presentation/rest/InternalStockReservationController.java`
- **Base Path:** `/api/v1/internal/inventory/reservations`
- **Class Tag:** `Internal Stock Reservations`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/reserve` | Reserve stock for checkout | `201, 400, 422` | COMPLIANT |
| `PostMapping` | `/confirm` | Confirm stock reservation | `200, 404, 422` | COMPLIANT |
| `PostMapping` | `/release` | Release stock reservation | `200, 404` | COMPLIANT |
| `PostMapping` | `/cancel` | Cancel stock reservation | `200, 404` | COMPLIANT |
| `PostMapping` | `/sweep-expired` | Sweep expired reservations | `200` | COMPLIANT |

#### PublicStockAvailabilityController
- **File:** `src/main/java/com/bazaarhub/backend/inventory/presentation/rest/PublicStockAvailabilityController.java`
- **Base Path:** `/api/v1/products/{productId}/variants/{variantId}/availability`
- **Class Tag:** `Public Product Availability`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (1):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Check public stock availability | `200, 404` | PUBLIC |

#### SellerInventoryController
- **File:** `src/main/java/com/bazaarhub/backend/inventory/presentation/rest/SellerInventoryController.java`
- **Base Path:** `/api/v1/seller/inventory`
- **Class Tag:** `Seller Inventory`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List seller inventory | `200, 403` | COMPLIANT |
| `GetMapping` | `/{variantId}` | Get variant inventory | `200, 403, 404` | COMPLIANT |
| `PatchMapping` | `/{variantId}/safety-stock` | Update safety stock threshold | `200, 400, 403, 404` | COMPLIANT |
| `PostMapping` | `/{variantId}/stock-in` | Stock replenishment (Stock In) | `200, 400, 403, 404` | COMPLIANT |
| `PostMapping` | `/{variantId}/stock-out` | Stock reduction (Stock Out) | `200, 400, 403, 404, 422` | COMPLIANT |
| `PostMapping` | `/{variantId}/adjustment` | Physical stock adjustment | `200, 400, 403, 404, 422` | COMPLIANT |
| `GetMapping` | `/{variantId}/movements` | View variant movement history | `200, 403, 404` | COMPLIANT |

### Module: `notification` (3 Controllers)

#### AdminNotificationController
- **File:** `src/main/java/com/bazaarhub/backend/notification/presentation/rest/AdminNotificationController.java`
- **Base Path:** `/api/v1/admin/notifications`
- **Class Tag:** `Admin Notifications & Templates`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (6):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Query all notifications | `NONE` | DEFICIENT |
| `PostMapping` | `/send` | Send or test notification | `NONE` | DEFICIENT |
| `GetMapping` | `/templates` | List notification templates | `NONE` | DEFICIENT |
| `GetMapping` | `/templates/{id}` | Get notification template by ID | `NONE` | DEFICIENT |
| `PostMapping` | `/templates` | Create notification template | `NONE` | DEFICIENT |
| `PutMapping` | `/templates/{id}` | Update notification template | `NONE` | DEFICIENT |

#### CustomerNotificationController
- **File:** `src/main/java/com/bazaarhub/backend/notification/presentation/rest/CustomerNotificationController.java`
- **Base Path:** `/api/v1/customer`
- **Class Tag:** `Customer Notifications`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (6):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/notifications` | List customer notifications | `NONE` | DEFICIENT |
| `GetMapping` | `/notifications/unread-count` | Get unread notification count | `NONE` | DEFICIENT |
| `PatchMapping` | `/notifications/{id}/read` | Mark notification as read | `NONE` | DEFICIENT |
| `PatchMapping` | `/notifications/read-all` | Mark all notifications as read | `NONE` | DEFICIENT |
| `GetMapping` | `/notification-preferences` | Get customer notification preferences | `NONE` | DEFICIENT |
| `PutMapping` | `/notification-preferences` | Update customer notification preferences | `NONE` | DEFICIENT |

#### SellerNotificationController
- **File:** `src/main/java/com/bazaarhub/backend/notification/presentation/rest/SellerNotificationController.java`
- **Base Path:** `/api/v1/seller`
- **Class Tag:** `Seller Notifications`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (6):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/notifications` | List seller notifications | `NONE` | DEFICIENT |
| `GetMapping` | `/notifications/unread-count` | Get seller unread notification count | `NONE` | DEFICIENT |
| `PatchMapping` | `/notifications/{id}/read` | Mark seller notification as read | `NONE` | DEFICIENT |
| `PatchMapping` | `/notifications/read-all` | Mark all seller notifications as read | `NONE` | DEFICIENT |
| `GetMapping` | `/notification-preferences` | Get seller notification preferences | `NONE` | DEFICIENT |
| `PutMapping` | `/notification-preferences` | Update seller notification preferences | `NONE` | DEFICIENT |

### Module: `order` (3 Controllers)

#### AdminOrderController
- **File:** `src/main/java/com/bazaarhub/backend/order/presentation/rest/AdminOrderController.java`
- **Base Path:** `/api/v1/admin/orders`
- **Class Tag:** `Admin Order Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Search and list all orders | `200, 401, 403` | COMPLIANT |
| `GetMapping` | `/{orderId}` | Get complete order tree | `200, 404` | COMPLIANT |
| `PatchMapping` | `/{orderId}/status` | Administratively update order status | `200, 400, 404` | COMPLIANT |
| `GetMapping` | `/seller-orders` | List all seller sub-orders | `200` | COMPLIANT |
| `GetMapping` | `/{orderId}/history` | Get order audit history | `200, 404` | COMPLIANT |

#### CustomerOrderController
- **File:** `src/main/java/com/bazaarhub/backend/order/presentation/rest/CustomerOrderController.java`
- **Base Path:** `/api/v1/orders`
- **Class Tag:** `Customer Orders`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Place order from active cart | `201, 400, 401` | COMPLIANT |
| `GetMapping` | `/` | List customer orders | `200, 401` | COMPLIANT |
| `GetMapping` | `/{orderId}` | Get order details by ID | `200, 403, 404` | COMPLIANT |
| `GetMapping` | `/by-number/{orderNumber}` | Get order details by order number | `200, 403, 404` | COMPLIANT |
| `PostMapping` | `/{orderId}/cancel` | Cancel an order | `200, 400, 403, 404` | COMPLIANT |

#### SellerOrderController
- **File:** `src/main/java/com/bazaarhub/backend/order/presentation/rest/SellerOrderController.java`
- **Base Path:** `/api/v1/seller/orders`
- **Class Tag:** `Seller Order Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List seller sub-orders | `200, 401, 403` | COMPLIANT |
| `GetMapping` | `/{sellerOrderId}` | Get seller sub-order by ID | `200, 403, 404` | COMPLIANT |
| `GetMapping` | `/by-number/{subOrderNumber}` | Get seller sub-order by sub-order number | `200, 403, 404` | COMPLIANT |
| `PatchMapping` | `/{sellerOrderId}/status` | Update seller sub-order fulfillment status | `200, 400, 403, 404` | COMPLIANT |
| `PostMapping` | `/{sellerOrderId}/cancel` | Cancel seller sub-order | `200, 400, 403, 404` | COMPLIANT |

### Module: `payment` (3 Controllers)

#### AdminPaymentController
- **File:** `src/main/java/com/bazaarhub/backend/payment/presentation/rest/AdminPaymentController.java`
- **Base Path:** `/api/v1/admin`
- **Class Tag:** `Admin Payments & Refunds`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/payments/{paymentId}/cod-collect` | Collect Cash on Delivery payment | `200, 400, 404` | COMPLIANT |
| `PostMapping` | `/payments/{paymentId}/refunds` | Initiate payment refund | `201, 400, 404` | COMPLIANT |
| `GetMapping` | `/payments` | List all payments | `200` | COMPLIANT |
| `GetMapping` | `/refunds` | List all refunds | `200` | COMPLIANT |
| `GetMapping` | `/refunds/{refundId}` | Get refund details by ID | `200, 404` | COMPLIANT |

#### CustomerPaymentController
- **File:** `src/main/java/com/bazaarhub/backend/payment/presentation/rest/CustomerPaymentController.java`
- **Base Path:** `/api/v1/payments`
- **Class Tag:** `Customer Payments`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (8):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/razorpay/create-order` | Initiate Razorpay checkout order | `201, 400, 403, 404` | COMPLIANT |
| `PostMapping` | `/razorpay/verify` | Verify Razorpay checkout signature | `200, 400, 403, 404` | COMPLIANT |
| `PostMapping` | `/cod/select` | Select Cash on Delivery (COD) | `200, 400, 403, 404` | COMPLIANT |
| `GetMapping` | `/{paymentId}` | Get payment details by ID | `200, 403, 404` | COMPLIANT |
| `GetMapping` | `/order/{orderId}` | Get payment details by Order ID | `200, 403, 404` | COMPLIANT |
| `GetMapping` | `/` | List customer payments | `200` | COMPLIANT |
| `GetMapping` | `/{paymentId}/refunds` | List refunds for a payment | `200, 403, 404` | COMPLIANT |
| `GetMapping` | `/refunds` | List customer refunds | `200` | COMPLIANT |

#### PaymentWebhookController
- **File:** `src/main/java/com/bazaarhub/backend/payment/presentation/rest/PaymentWebhookController.java`
- **Base Path:** `/api/v1/payments/webhooks`
- **Class Tag:** `Payment Webhooks`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (1):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/razorpay` | Receive Razorpay webhook event | `200, 400` | DEFICIENT |

### Module: `review` (4 Controllers)

#### AdminReviewController
- **File:** `src/main/java/com/bazaarhub/backend/review/presentation/rest/AdminReviewController.java`
- **Base Path:** `/api/v1/admin/reviews`
- **Class Tag:** `Admin Review Moderation`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (4):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Get all reviews | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}` | Get review details | `NONE` | DEFICIENT |
| `PatchMapping` | `/{id}/moderate` | Moderate product review | `NONE` | DEFICIENT |
| `DeleteMapping` | `/{id}` | Delete review | `NONE` | DEFICIENT |

#### CustomerReviewController
- **File:** `src/main/java/com/bazaarhub/backend/review/presentation/rest/CustomerReviewController.java`
- **Base Path:** `/api/v1/customer/reviews`
- **Class Tag:** `Customer Reviews`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Create product review | `NONE` | DEFICIENT |
| `PutMapping` | `/{id}` | Update product review | `NONE` | DEFICIENT |
| `DeleteMapping` | `/{id}` | Delete product review | `NONE` | DEFICIENT |
| `GetMapping` | `/` | Get customer reviews | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}` | Get review by ID | `NONE` | DEFICIENT |

#### PublicReviewController
- **File:** `src/main/java/com/bazaarhub/backend/review/presentation/rest/PublicReviewController.java`
- **Base Path:** `/api/v1/products/{productId}`
- **Class Tag:** `Product Reviews (Public)`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (2):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/reviews` | Get public product reviews | `NONE` | PUBLIC |
| `GetMapping` | `/rating-summary` | Get product rating summary | `NONE` | PUBLIC |

#### SellerReviewController
- **File:** `src/main/java/com/bazaarhub/backend/review/presentation/rest/SellerReviewController.java`
- **Base Path:** `/api/v1/seller/reviews`
- **Class Tag:** `Seller Reviews`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (1):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Get seller product reviews | `NONE` | DEFICIENT |

### Module: `search` (2 Controllers)

#### ProductDiscoveryController
- **File:** `src/main/java/com/bazaarhub/backend/search/presentation/rest/ProductDiscoveryController.java`
- **Base Path:** `/api/v1/discovery`
- **Class Tag:** `Product Discovery & Recommendations`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (4):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/featured` | Get featured products | `200, 400` | PUBLIC |
| `GetMapping` | `/new-arrivals` | Get new arrival products | `200, 400` | PUBLIC |
| `GetMapping` | `/related/{productId}` | Get related products | `200, 400, 404` | PUBLIC |
| `GetMapping` | `/by-category/{categoryId}` | Get recommended products by category | `200, 400, 404` | PUBLIC |

#### ProductSearchController
- **File:** `src/main/java/com/bazaarhub/backend/search/presentation/rest/ProductSearchController.java`
- **Base Path:** `/api/v1/search`
- **Class Tag:** `Product Search & Catalog Discovery`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (2):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/products` | Search public catalog products | `200, 400` | PUBLIC |
| `GetMapping` | `/categories/{categoryId}/products` | Browse products by category | `200, 400, 404` | PUBLIC |

### Module: `seller` (5 Controllers)

#### AdminSellerController
- **File:** `src/main/java/com/bazaarhub/backend/seller/presentation/rest/AdminSellerController.java`
- **Base Path:** `/api/v1/admin/sellers`
- **Class Tag:** `Admin Seller Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (6):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List seller applications with filtering and pagination | `200, 401, 403` | COMPLIANT |
| `GetMapping` | `/{sellerId}` | Get full seller application dossier | `200, 401, 403, 404` | COMPLIANT |
| `PostMapping` | `/{sellerId}/approve` | Approve seller application | `200, 400, 401, 403, 404` | COMPLIANT |
| `PostMapping` | `/{sellerId}/reject` | Reject seller application | `200, 400, 401, 403, 404` | COMPLIANT |
| `PostMapping` | `/{sellerId}/suspend` | Suspend seller account and storefront | `200, 401, 403, 404` | COMPLIANT |
| `PostMapping` | `/{sellerId}/verify-documents` | Review and verify KYC document | `200, 400, 401, 403, 404` | COMPLIANT |

#### SellerBankController
- **File:** `src/main/java/com/bazaarhub/backend/seller/presentation/rest/SellerBankController.java`
- **Base Path:** `/api/v1/sellers/me/bank`
- **Class Tag:** `Seller Settlement Bank Accounts`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Submit settlement bank account | `201, 400, 401, 404` | COMPLIANT |
| `GetMapping` | `/` | List seller bank accounts | `200, 401, 404` | COMPLIANT |
| `GetMapping` | `/{bankId}` | Get seller bank account by ID | `200, 401, 403, 404` | COMPLIANT |

#### SellerController
- **File:** `src/main/java/com/bazaarhub/backend/seller/presentation/rest/SellerController.java`
- **Base Path:** `/api/v1/sellers`
- **Class Tag:** `Seller Onboarding & Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (4):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/onboarding` | Submit seller onboarding application | `201, 400, 401, 409` | COMPLIANT |
| `GetMapping` | `/me` | Get current seller profile | `200, 401, 404` | COMPLIANT |
| `PutMapping` | `/me` | Update seller business profile | `200, 400, 401, 404` | COMPLIANT |
| `GetMapping` | `/me/status` | Get seller onboarding lifecycle status | `200, 401, 404` | COMPLIANT |

#### SellerDocumentController
- **File:** `src/main/java/com/bazaarhub/backend/seller/presentation/rest/SellerDocumentController.java`
- **Base Path:** `/api/v1/sellers/me/documents`
- **Class Tag:** `Seller KYC Documents`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Submit KYC verification document | `201, 400, 401, 404` | COMPLIANT |
| `GetMapping` | `/` | List seller KYC documents | `200, 401, 404` | COMPLIANT |
| `GetMapping` | `/{documentId}` | Get seller KYC document by ID | `200, 401, 403, 404` | COMPLIANT |

#### StoreController
- **File:** `src/main/java/com/bazaarhub/backend/seller/presentation/rest/StoreController.java`
- **Base Path:** `/api/v1/sellers/me/store`
- **Class Tag:** `Storefront Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (3):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Create merchant storefront | `201, 400, 401, 404, 409` | COMPLIANT |
| `GetMapping` | `/` | Get merchant storefront | `200, 401, 404` | COMPLIANT |
| `PutMapping` | `/` | Update merchant storefront | `200, 400, 401, 404` | COMPLIANT |

### Module: `shared` (1 Controllers)

#### HealthController
- **File:** `src/main/java/com/bazaarhub/backend/shared/presentation/rest/health/HealthController.java`
- **Base Path:** `/api/v1`
- **Class Tag:** `MISSING`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (1):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/health` | MISSING @Operation | `NONE` | DEFICIENT |

### Module: `shipping` (5 Controllers)

#### AdminShipmentController
- **File:** `src/main/java/com/bazaarhub/backend/shipping/presentation/rest/AdminShipmentController.java`
- **Base Path:** `/api/v1/admin/shipments`
- **Class Tag:** `Admin Shipment Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List all platform shipments | `NONE` | DEFICIENT |
| `GetMapping` | `/{shipmentId}` | Get shipment details | `NONE` | DEFICIENT |
| `GetMapping` | `/active` | List active in-flight shipments | `NONE` | DEFICIENT |
| `GetMapping` | `/failed` | List failed shipments | `NONE` | DEFICIENT |
| `PostMapping` | `/{shipmentId}/cancel` | Cancel shipment | `NONE` | DEFICIENT |

#### CustomerShipmentController
- **File:** `src/main/java/com/bazaarhub/backend/shipping/presentation/rest/CustomerShipmentController.java`
- **Base Path:** `/api/v1/customer/shipments`
- **Class Tag:** `Customer Shipments`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (2):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List customer shipments | `NONE` | DEFICIENT |
| `GetMapping` | `/{shipmentId}` | Get customer shipment details | `NONE` | DEFICIENT |

#### InternalShipmentController
- **File:** `src/main/java/com/bazaarhub/backend/shipping/presentation/rest/InternalShipmentController.java`
- **Base Path:** `/api/v1/shipments`
- **Class Tag:** `Internal Shipping Operations`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (5):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Create shipment for seller sub-order | `NONE` | DEFICIENT |
| `GetMapping` | `/{shipmentId}` | Get shipment details by ID | `NONE` | DEFICIENT |
| `GetMapping` | `/order/{orderId}` | Get shipments for customer order | `NONE` | DEFICIENT |
| `PostMapping` | `/{shipmentId}/ready-for-pickup` | Mark parcel ready for pickup | `NONE` | DEFICIENT |
| `PostMapping` | `/rate-quote` | Calculate shipping rate quote | `NONE` | DEFICIENT |

#### SellerShipmentController
- **File:** `src/main/java/com/bazaarhub/backend/shipping/presentation/rest/SellerShipmentController.java`
- **Base Path:** `/api/v1/shipments/seller`
- **Class Tag:** `Seller Shipment Operations`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (2):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | List seller shipments | `NONE` | DEFICIENT |
| `GetMapping` | `/{shipmentId}` | Get seller shipment details | `NONE` | DEFICIENT |

#### ShipmentTrackingController
- **File:** `src/main/java/com/bazaarhub/backend/shipping/presentation/rest/ShipmentTrackingController.java`
- **Base Path:** `/api/v1/shipments`
- **Class Tag:** `Shipment Tracking`
- **Security Scheme:** `None (public or missing)`
- **Endpoints (2):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/track/{trackingNumber}` | Public shipment tracking lookup | `NONE` | DEFICIENT |
| `GetMapping` | `/{shipmentId}/tracking-history` | Get full tracking history | `NONE` | DEFICIENT |

### Module: `support` (4 Controllers)

#### AdminSupportController
- **File:** `src/main/java/com/bazaarhub/backend/support/presentation/rest/AdminSupportController.java`
- **Base Path:** `/api/v1/admin/support/tickets`
- **Class Tag:** `Admin Support & Helpdesk Desk`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (9):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Query all support tickets | `NONE` | DEFICIENT |
| `GetMapping` | `/stats` | Get ticket lifecycle metrics | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}` | Get complete ticket details | `NONE` | DEFICIENT |
| `PatchMapping` | `/{id}/assign` | Assign ticket to support agent | `NONE` | DEFICIENT |
| `PatchMapping` | `/{id}/status` | Update ticket lifecycle status | `NONE` | DEFICIENT |
| `PatchMapping` | `/{id}/priority` | Update ticket priority | `NONE` | DEFICIENT |
| `PostMapping` | `/{id}/messages` | Post agent reply or private internal note | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}/messages` | Get all ticket messages (including internal notes) | `NONE` | DEFICIENT |
| `PostMapping` | `/attachments` | Upload staff attachment | `NONE` | DEFICIENT |

#### CustomerSupportController
- **File:** `src/main/java/com/bazaarhub/backend/support/presentation/rest/CustomerSupportController.java`
- **Base Path:** `/api/v1/customer/tickets`
- **Class Tag:** `Customer Support & Tickets`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Submit support ticket | `NONE` | DEFICIENT |
| `GetMapping` | `/` | List customer tickets | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}` | Get ticket details | `NONE` | DEFICIENT |
| `PostMapping` | `/{id}/messages` | Reply to ticket | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}/messages` | Get ticket messages | `NONE` | DEFICIENT |
| `PatchMapping` | `/{id}/close` | Close ticket | `NONE` | DEFICIENT |
| `PostMapping` | `/attachments` | Upload support attachment | `NONE` | DEFICIENT |

#### DeliverySupportController
- **File:** `src/main/java/com/bazaarhub/backend/support/presentation/rest/DeliverySupportController.java`
- **Base Path:** `/api/v1/delivery/tickets`
- **Class Tag:** `Delivery Partner Support & Tickets`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Submit delivery partner support ticket | `NONE` | DEFICIENT |
| `GetMapping` | `/` | List delivery partner tickets | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}` | Get delivery ticket details | `NONE` | DEFICIENT |
| `PostMapping` | `/{id}/messages` | Reply to delivery ticket | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}/messages` | Get delivery ticket messages | `NONE` | DEFICIENT |
| `PatchMapping` | `/{id}/close` | Close delivery ticket | `NONE` | DEFICIENT |
| `PostMapping` | `/attachments` | Upload delivery obstacle proof | `NONE` | DEFICIENT |

#### SellerSupportController
- **File:** `src/main/java/com/bazaarhub/backend/support/presentation/rest/SellerSupportController.java`
- **Base Path:** `/api/v1/seller/tickets`
- **Class Tag:** `Seller Support & Tickets`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `PostMapping` | `/` | Submit merchant support ticket | `NONE` | DEFICIENT |
| `GetMapping` | `/` | List seller tickets | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}` | Get seller ticket details | `NONE` | DEFICIENT |
| `PostMapping` | `/{id}/messages` | Reply to seller ticket | `NONE` | DEFICIENT |
| `GetMapping` | `/{id}/messages` | Get seller ticket messages | `NONE` | DEFICIENT |
| `PatchMapping` | `/{id}/close` | Close seller ticket | `NONE` | DEFICIENT |
| `PostMapping` | `/attachments` | Upload seller attachment | `NONE` | DEFICIENT |

### Module: `wishlist` (1 Controllers)

#### CustomerWishlistController
- **File:** `src/main/java/com/bazaarhub/backend/wishlist/presentation/rest/CustomerWishlistController.java`
- **Base Path:** `/api/v1/customer/wishlist`
- **Class Tag:** `Customer Wishlist Management`
- **Security Scheme:** `bearerAuth (class-level)`
- **Endpoints (7):**

| HTTP Verb | Subpath | Summary | Documented Status Codes | Status |
|:---|:---|:---|:---|:---|
| `GetMapping` | `/` | Get customer wishlist | `200, 401` | COMPLIANT |
| `PutMapping` | `/` | Rename customer wishlist | `200, 400, 401` | COMPLIANT |
| `GetMapping` | `/items` | Get wishlist items | `200, 401` | COMPLIANT |
| `PostMapping` | `/items` | Add product to wishlist | `201, 400, 401, 404, 409` | COMPLIANT |
| `DeleteMapping` | `/items/{productId}` | Remove product from wishlist | `200, 401, 404` | COMPLIANT |
| `GetMapping` | `/items/{productId}/exists` | Check if product is in wishlist | `200, 401` | COMPLIANT |
| `DeleteMapping` | `/items` | Clear customer wishlist | `200, 401` | COMPLIANT |
