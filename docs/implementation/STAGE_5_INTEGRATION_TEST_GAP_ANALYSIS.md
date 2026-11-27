# Stage 5 Integration Test Coverage Audit & Gap Analysis
**Document:** `STAGE_5_INTEGRATION_TEST_GAP_ANALYSIS.md`  
**Stage:** Pre-Phase 16 Remediation – Stage 5 (REM-TEST-01)  
**Date:** 2026-11-18  
**Scope:** BazaarHub Multi-Vendor Backend (Java 21 / Spring Boot 4.1.2)

---

## 1. Executive Summary

As part of Pre-Phase 16 Remediation Stage 5 (`REM-SEC-04` + `REM-TEST-01`), this audit evaluates integration test coverage across all 59 Spring Boot REST controllers in BazaarHub. While core domains (Auth, Customer, Cart, Order, Catalog Storefront, Inventory) possess integration suites, significant gaps were discovered in administrative oversight and partner operations—specifically **Admin Seller KYC / Life-cycle**, **Delivery Partner Operations (`DeliveryBoyController`)**, and **Admin Payment / Refund Ledger Management (`AdminPaymentController`)**.

---

## 2. Current Integration Test Inventory (Baseline: 24 Suites)

| Test Suite Class | Domain / Module | Primary Controllers Covered | Coverage Depth |
|---|---|---|---|
| `AuthControllerIntegrationTest` | Identity | `AuthController` | Comprehensive |
| `CustomerIntegrationTest` | Customer | `CustomerProfileController`, `CustomerAddressController` | High |
| `CatalogControllerIntegrationTest` | Catalog | `CategoryController`, `BrandController` | High |
| `ProductControllerIntegrationTest` | Catalog | `ProductController`, `SellerProductController` | High |
| `SellerProductCatalogSecurityIntegrationTest` | Catalog | `SellerProductVariantController`, `SellerProductImageController` | Complete (BOLA / 403 / 404 / RBAC) |
| `ProductSearchIntegrationTest` | Search | `ProductSearchController` | High |
| `ProductReviewIntegrationTest` | Review | `ProductReviewController` | High |
| `CartIntegrationTest` | Cart | `CartController` | Comprehensive |
| `CheckoutIntegrationTest` | Checkout | `CheckoutController`, `CustomerCheckoutController` | High |
| `OrderIntegrationTest` | Order | `OrderController`, `CustomerOrderController`, `SellerOrderController` | Comprehensive |
| `InternalShippingIntegrationTest` | Shipping | `InternalShipmentController` | High |
| `DeliveryIntegrationTest` | Delivery | End-to-end multi-step workflow | Single macro workflow |
| `CustomerDeliveryTrackingSecurityIntegrationTest` | Delivery | `CustomerDeliveryTrackingController` | Complete (BOLA / IDOR defense) |
| `InventoryIntegrationTest` | Inventory | `InventoryController`, `SellerInventoryController`, `AdminInventoryController` | High (15 tests, RBAC, BOLA, lifecycle) |
| `InventoryConcurrencyIntegrationTest` | Inventory | Optimistic/pessimistic locking | High (Concurrency stress) |
| `PaymentIntegrationTest` | Payment | `CustomerPaymentController`, `PaymentWebhookController` | High |
| `SupportTicketIntegrationTest` | Support | `CustomerSupportController`, `SellerSupportController`, `AdminSupportController` | High |
| `NotificationIntegrationTest` | Notification | `NotificationController` | Medium |
| `AnalyticsIntegrationTest` | Analytics | `SellerAnalyticsController`, `AdminAnalyticsController` | High |
| `AiIntegrationTest` | AI | `AiController` | Mocked high |
| `SellerControllerIntegrationTest` | Seller | `SellerController` (onboarding & self-approval check) | Minimal (2 tests) |
| `RateLimitingFilterIntegrationTest` | Shared/Infra | Distributed & Memory Rate Limiting Filter | Complete (23 tests) |
| `PaginationLimitSecurityIntegrationTest` | Shared/Web | Global Page Size Clamping Customizer | Complete (22 tests) |
| `RequestBodyValidationIntegrationTest` | Shared/Validation | Global DTO Bean Validation | Complete (25 tests) |

---

## 3. Identified Critical Gaps (REM-TEST-01)

### Gap 1: Administrative Seller Lifecycle & Document Verification (`AdminSellerController`)
- **Controller:** `com.bazaarhub.backend.seller.presentation.rest.AdminSellerController`
- **Current Coverage:** `SellerControllerIntegrationTest` only tested basic onboarding and a self-approval check.
- **Missing Endpoints:**
  - `GET /api/v1/admin/sellers`: Paginated seller application list with `approvalStatus` and `verificationStatus` filtering.
  - `GET /api/v1/admin/sellers/{sellerId}`: Detailed seller dossier with storefront and documents.
  - `POST /api/v1/admin/sellers/{sellerId}/approve`: Seller approval, role grant, storefront activation.
  - `POST /api/v1/admin/sellers/{sellerId}/reject`: Rejection with required justification.
  - `POST /api/v1/admin/sellers/{sellerId}/suspend`: Suspension with reason.
  - `POST /api/v1/admin/sellers/{sellerId}/verify-documents`: Reviewing individual KYC documents (`VERIFIED` / `REJECTED`) and aggregate verification status promotion.
  - Non-admin / Customer unauthorized access rejection (HTTP 403) and unauthenticated rejection (HTTP 401).

### Gap 2: Delivery Partner Operations & Assignment Lifecycle (`DeliveryBoyController` & `AdminDeliveryController`)
- **Controllers:**
  - `com.bazaarhub.backend.delivery.presentation.rest.DeliveryBoyController`
  - `com.bazaarhub.backend.delivery.presentation.rest.AdminDeliveryController`
- **Current Coverage:** `DeliveryIntegrationTest` tests a single happy-path workflow; individual endpoints and edge cases lack isolated test coverage.
- **Missing Endpoints & Scenarios:**
  - `POST /api/v1/delivery/onboard`: Onboarding as delivery partner, vehicle details, role assignment.
  - `GET /api/v1/delivery/profile` & `PUT /api/v1/delivery/profile`: Partner profile inspection and updates.
  - `PATCH /api/v1/delivery/availability`: Availability toggle (`AVAILABLE` vs `OFFLINE`).
  - `POST /api/v1/admin/delivery/agents/{id}/verify`: Admin KYC verification of delivery boy.
  - `GET /api/v1/delivery/assignments`: Paginated list of assignments with status filter.
  - `POST /api/v1/delivery/assignments/{id}/accept` & `/reject`: Partner response to assignment.
  - `POST /api/v1/delivery/assignments/{id}/pickup`: Confirmation of pickup from merchant store.
  - `POST /api/v1/delivery/assignments/{id}/out-for-delivery`: Transition to out for delivery with OTP generation.
  - `POST /api/v1/delivery/assignments/{id}/complete`: OTP validation and COD settlement.
  - `POST /api/v1/delivery/assignments/{id}/fail`: Recording failed delivery attempt with reason.
  - `POST /api/v1/delivery/tracking/location`: Live GPS coordinate ping.
  - `POST /api/v1/delivery/proof/upload`: Delivery proof photo upload via Cloudinary adapter.
  - Role isolation: Customers and Sellers denied access (HTTP 403).

### Gap 3: Admin Payment & Refund Ledger Operations (`AdminPaymentController`)
- **Controller:** `com.bazaarhub.backend.payment.presentation.rest.AdminPaymentController`
- **Current Coverage:** `PaymentIntegrationTest` tested customer payment flow and webhook; administrative refund and COD collection were partially mixed in.
- **Missing Dedicated Suite:**
  - `POST /api/v1/admin/payments/{id}/cod-collect`: COD cash settlement reconciliation.
  - `POST /api/v1/admin/payments/{id}/refunds`: Razorpay / manual refund dispatch, amount validation, partial vs full refund.
  - `GET /api/v1/admin/payments`: Global payment ledger pagination and sorting.
  - `GET /api/v1/admin/refunds`: Platform refund audit log pagination.
  - `GET /api/v1/admin/refunds/{refundId}`: Specific refund details inspection.
  - Authorization guards: Customer/Seller access rejected with HTTP 403; Unauthenticated rejected with HTTP 401.

---

## 4. Remediation Plan (Phase E Implementation)

To close all identified gaps and satisfy `REM-TEST-01`, the following 3 dedicated integration test classes will be implemented:

1. `AdminSellerControllerIntegrationTest.java`:
   - Complete coverage of `AdminSellerController` endpoints (listing, dossier, document KYC verification, approval, rejection, suspension, RBAC defense).
2. `DeliveryBoyControllerIntegrationTest.java`:
   - Full coverage of `DeliveryBoyController` and `AdminDeliveryController` lifecycle (onboarding, profile, availability, assignments, state transitions, OTP delivery completion, GPS ping, proof upload, RBAC isolation).
3. `AdminPaymentIntegrationTest.java`:
   - Full coverage of `AdminPaymentController` (COD collection, refund creation, global payment pagination, refund ledger inspection, RBAC defense).

Inventory controllers are confirmed to already have comprehensive coverage via `InventoryIntegrationTest.java` (15 tests) and `InventoryConcurrencyIntegrationTest.java`.
