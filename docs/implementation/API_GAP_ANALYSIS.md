# BazaarHub — Enterprise Backend API Gap Analysis

> **Document Status**: Production Complete | **Audit Type**: Comprehensive Enterprise Gap Analysis  
> **Target System**: BazaarHub Enterprise Multi-Vendor E-Commerce Marketplace  
> **Baseline**: 286 Implemented Endpoints across 28 Modular Domains  
> **Architecture**: Java 21 | Spring Boot 4.1.2 | MariaDB 11.8.9 | Clean Architecture  

---

## 1. Executive Summary & Maturity Assessment

A deep architectural and functional inspection was performed on BazaarHub's existing 286 APIs across 59 controllers. While BazaarHub possesses a robust foundation covering authentication, RBAC, multi-vendor catalog, inventory locking, checkout, delivery fleet routing, payments (Razorpay), customer support, Groq AI assistant, and analytics, critical enterprise gaps exist that prevent full production readiness as a top-tier Indian multi-vendor marketplace.

### Maturity Assessment Scorecard:

| Domain Area | Current Baseline Score | Maturity Level | Primary Missing Capabilities |
| :--- | :---: | :---: | :--- |
| **Core Identity & RBAC** | 90% | Advanced | Missing user invite flows & 2FA/MFA |
| **Product & Catalog** | 85% | Production Ready | Missing bulk CSV imports, variant matrix generators |
| **Orders & Checkout** | 88% | Production Ready | Missing guest cart merge, split payment methods |
| **Payments & Webhooks** | 80% | Functional | Missing partial refunds, dispute/chargeback handling |
| **Delivery & Logistics** | 82% | Advanced | Missing live WebSocket GPS streaming, route optimization |
| **Customer Support (Phase 15B)** | 92% | Production Ready | Complete ticket lifecycle, needs CSAT ratings |
| **AI & Analytics (15C/15D)** | 88% | Advanced | High Groq utility, needs automated trend forecasting |
| **Promotions & Discounts** | **0%** | **Empty Scaffold** | **No coupon engine, no flash sales, no promo codes** |
| **Commission & Payouts** | **15%** | **Scaffold / Partial** | **No payout requests, no settlement ledger, no bank transfer** |
| **Returns & Reverse Logistics**| **15%** | **Scaffold / Partial** | **No return pickup dispatch, QC checks, exchanges** |
| **Taxation & GST Compliance** | **20%** | **Minimal** | **No HSN lookup, no automated CGST/SGST/IGST split, no GST invoice** |
| **Platform Administration** | **40%** | **Dispersed** | **No centralized audit log, no system settings, no maintenance mode** |

---

## 2. Part I — Missing Enterprise Modules & Business Domains

### 2.1. Promotions, Coupons & Discount Engine (`com.bazaarhub.backend.promotion`)
- **Current State**: Package exists as an empty Clean Architecture scaffold with `package-info.java` and `README.md`. No entities, repositories, or controllers exist.
- **Business Impact**: High severity. E-commerce conversion heavily depends on coupon codes, cart-level threshold discounts, category flash sales, and first-time buyer incentives.
- **Missing API Capabilities**:
  1. `POST /api/v1/cart/coupons/apply` — Apply promo code to active cart with atomic usage validation.
  2. `DELETE /api/v1/cart/coupons/remove` — Remove applied coupon and recalculate cart totals.
  3. `GET /api/v1/cart/coupons/applicable` — List eligible coupons for the customer's current cart value.
  4. `GET /api/v1/promotions/active` — Public listing of active promotional banners and flash sales.
  5. `POST /api/v1/seller/promotions` — Seller-funded discount creation (e.g., 10% off storewide).
  6. `GET /api/v1/seller/promotions` — Seller active and scheduled promotions list.
  7. `POST /api/v1/admin/coupons` — Platform-wide coupon creation (min cart value, max discount cap, expiry).
  8. `GET /api/v1/admin/coupons` — Admin coupon management and redemptions audit.

### 2.2. Commission Management & Seller Payouts (`com.bazaarhub.backend.commission` & `finance`)
- **Current State**: `SellerOrderEntity` records a calculated `commissionAmount`, and `SellerAnalyticsController` aggregates it. However, the `commission` and `finance` packages are empty scaffolds. There is NO mechanism for sellers to receive payout settlements or for the platform to manage commission rule tiers.
- **Business Impact**: Critical severity. Vendors cannot withdraw earnings. The platform cannot automate financial settlements.
- **Missing API Capabilities**:
  1. `GET /api/v1/seller/payouts` — Seller payout history, pending balances, and settlement breakdown.
  2. `POST /api/v1/seller/payouts/request` — Seller withdrawal request for eligible matured balances.
  3. `GET /api/v1/seller/payouts/{payoutId}/invoice` — Download payout statement / TDS deduction certificate.
  4. `GET /api/v1/admin/commissions/rules` — View dynamic commission rules by category and seller tier.
  5. `POST /api/v1/admin/commissions/rules` — Create/update category-based commission percentages.
  6. `GET /api/v1/admin/payouts/pending` — Admin queue of pending seller withdrawal requests.
  7. `POST /api/v1/admin/payouts/{payoutId}/process` — Trigger bank transfer via Razorpay Payouts API.
  8. `POST /api/v1/admin/payouts/{payoutId}/reject` — Reject payout request with audit reason.
  9. `GET /api/v1/admin/finance/ledger` — Master double-entry reconciliation ledger.

### 2.3. Returns, Reverse Logistics & Replacements (`com.bazaarhub.backend.refund`)
- **Current State**: Refund APIs exist under `com.bazaarhub.backend.payment.presentation.rest` (`CustomerPaymentController` and `AdminPaymentController`), but they execute instantaneous Razorpay refund calls without handling physical item returns, pickup logistics, or warehouse quality inspection.
- **Business Impact**: High severity. Marketplace fraud occurs if refunds are issued without returning items or if sellers cannot verify item condition before refund disbursement.
- **Missing API Capabilities**:
  1. `POST /api/v1/orders/{orderId}/returns` — Customer submits return request with item IDs, reason, and images.
  2. `GET /api/v1/customer/returns` — Customer returns history and reverse shipment tracking.
  3. `GET /api/v1/customer/returns/{returnId}` — Detailed return progress (Approved, Pickup Scheduled, In Transit, QC Passed, Refunded).
  4. `GET /api/v1/seller/returns` — Seller dashboard of pending customer return requests.
  5. `POST /api/v1/seller/returns/{returnId}/approve` — Seller approves return authorization.
  6. `POST /api/v1/seller/returns/{returnId}/qc-complete` — Seller/Warehouse records Quality Check outcome (Passed -> release refund; Failed -> reject/return to buyer).
  7. `GET /api/v1/delivery/returns/assigned` — Delivery partner reverse pickup tasks.
  8. `POST /api/v1/delivery/returns/{returnId}/pickup-complete` — Delivery agent verifies item, collects return, verifies customer OTP.

### 2.4. Taxation, HSN & GST Compliance (India Specific)
- **Current State**: Variants store a flat `taxRate` percentage. No Indian GST structure is modeled.
- **Business Impact**: Regulatory compliance blocker. Under Indian tax laws, e-commerce operators must deduct TCS (Tax Collected at Source 1%), validate seller GSTIN, and calculate intra-state (CGST + SGST) vs inter-state (IGST) tax amounts dynamically.
- **Missing API Capabilities**:
  1. `GET /api/v1/tax/hsn-lookup` — Search HSN/SAC codes and standard GST rate slabs (0%, 5%, 12%, 18%, 28%).
  2. `POST /api/v1/seller/tax/validate-gstin` — Verify seller GSTIN format and state code.
  3. `GET /api/v1/orders/{orderId}/tax-invoice` — Generate compliant PDF tax invoice with IRN/QR code and CGST/SGST/IGST breakdown.
  4. `GET /api/v1/seller/reports/gstr1` — Export GSTR-1 compliant sales register for monthly filing.
  5. `GET /api/v1/admin/tax/tcs-reports` — Export Section 52 TCS report for government submission.

### 2.5. Centralized Platform Administration (`com.bazaarhub.backend.admin`)
- **Current State**: `com.bazaarhub.backend.admin` is an empty package. Existing admin endpoints are scattered across 16 domain controllers (`AdminOrderController`, `AdminDeliveryController`, `AdminSellerController`, etc.). While domain-specific admin controllers follow DDD, there are NO endpoints for overall platform configuration, audit trails, staff permissions, or emergency controls.
- **Missing API Capabilities**:
  1. `GET /api/v1/admin/system/settings` — Read global settings (marketplace name, contact email, default currency, delivery radius).
  2. `PUT /api/v1/admin/system/settings` — Update global settings.
  3. `GET /api/v1/admin/audit-logs` — Paged query of administrative actions with actor, IP, timestamp, and diff.
  4. `GET /api/v1/admin/staff` — List administrative staff accounts and assigned roles.
  5. `POST /api/v1/admin/staff/invite` — Provision support agent / catalog moderator accounts.
  6. `PUT /api/v1/admin/staff/{id}/roles` — Update staff roles.
  7. `POST /api/v1/admin/system/maintenance-mode` — Toggle maintenance mode with whitelist IPs.

### 2.6. Bulk Catalog Operations & Data Import/Export
- **Current State**: Products, variants, and inventory can only be created and updated one item at a time.
- **Missing API Capabilities**:
  1. `POST /api/v1/seller/products/bulk-upload` — Asynchronous bulk product creation via CSV/Excel.
  2. `GET /api/v1/seller/products/bulk-upload/{jobId}/status` — Poll background upload progress and error logs.
  3. `POST /api/v1/seller/inventory/bulk-update` — Bulk update stock levels across hundreds of SKUs.
  4. `GET /api/v1/seller/orders/export` — Export seller orders to CSV for third-party ERP integration.
  5. `GET /api/v1/admin/orders/export` — Platform-wide order export for financial audits.

---

## 3. Part II — Missing Operations in Existing 28 Modules

| Existing Module | Current Endpoint Count | Missing Functional Endpoints & Edge Cases |
| :--- | :---: | :--- |
| **Customer Wishlist** | 7 | Missing `POST /api/v1/customer/wishlist/move-to-cart/{itemId}` (move item atomically to cart) and `POST /api/v1/customer/wishlist/clear` |
| **Shopping Cart** | 6 | Missing `POST /api/v1/cart/merge` (merge guest cart into authenticated cart upon login) and `POST /api/v1/cart/items/{id}/save-for-later` |
| **Customer Orders** | 5 | Missing `POST /api/v1/orders/{orderId}/cancel-reason` (capture structured cancellation reason) and `GET /api/v1/orders/{orderId}/timeline` |
| **Seller Orders** | 5 | Missing `POST /api/v1/seller/orders/{orderId}/pack` (transition confirmed to packed) and `POST /api/v1/seller/orders/batch-confirm` |
| **Delivery Fleet** | 29 | Missing real-time WebSocket connection for live agent location streaming (currently polling only) and delivery agent zone assignment |
| **Reviews & Ratings** | 12 | Missing `POST /api/v1/customer/reviews/{reviewId}/vote` (Helpful/Unhelpful count) and `POST /api/v1/customer/reviews/{reviewId}/report` (Flag abuse) |
| **Notifications** | 18 | Missing `PATCH /api/v1/customer/notifications/mark-all-read` (bulk read) and `GET /api/v1/customer/notifications/unread-count` |
| **Customer Support** | 30 | Missing `POST /api/v1/customer/tickets/{id}/rate` (CSAT survey upon resolution) and `POST /api/v1/admin/support/tickets/{id}/escalate` |
| **Search & Discovery** | 6 | Missing `GET /api/v1/search/autocomplete` (typeahead suggestions) and `GET /api/v1/discovery/trending-tags` |
| **Payment Gateway** | 1 | Webhook handles only basic payment events; missing chargeback, dispute, and refund failure webhooks |

---

## 4. Part III — Quality, Validation & Architecture Gaps

### 4.1. Missing `@Valid` on `@RequestBody` Endpoints (10 Endpoints Identified)
Static analysis revealed that out of 106 endpoints consuming `@RequestBody`, 10 lack `@Valid` or `@Validated` annotations:

1. `POST /api/v1/admin/sellers/{sellerId}/suspend` (`AdminSellerController.suspendSeller`) — Suspension reason request body is unvalidated.
2. `POST /api/v1/payments/webhooks/razorpay` (`PaymentWebhookController.handleRazorpayWebhook`) — Intentionally unvalidated raw payload to enable cryptographic signature check (Valid design).
3. `POST /api/v1/shipments/{shipmentId}/ready-for-pickup` (`InternalShipmentController.markReadyForPickup`) — Needs `@Valid` to enforce pickup slot rules.
4. `POST /api/v1/shipments/rate-quote` (`InternalShipmentController.calculateRate`) — Needs `@Valid` to ensure weight and pincodes are positive.
5. `POST /api/v1/admin/support/tickets/attachments` (`AdminSupportController.uploadAttachment`) — File upload wrapper lacks constraint validation.
6. `POST /api/v1/customer/tickets/attachments` (`CustomerSupportController.uploadAttachment`) — File upload wrapper lacks constraint validation.
7. `POST /api/v1/delivery/tickets/attachments` (`DeliverySupportController.uploadAttachment`) — File upload wrapper lacks constraint validation.
8. `POST /api/v1/seller/tickets/attachments` (`SellerSupportController.uploadAttachment`) — File upload wrapper lacks constraint validation.
9. `POST /api/v1/checkout/validate` (`CustomerCheckoutController.validateCheckout`) — Validation request DTO should be annotated with `@Valid`.
10. `POST /api/v1/delivery/proof/upload` (`DeliveryBoyController.uploadProof`) — Proof upload metadata lacks `@Valid`.

### 4.2. Pagination Safety & Limit Clamping
- **Finding**: 56 endpoints accept Spring Data `Pageable` parameters.
- **Vulnerability**: While repository queries use `LIMIT` and `OFFSET`, several controllers do not enforce a maximum page size cap (e.g. `Math.min(pageable.getPageSize(), 100)`). A malicious client requesting `?size=1000000` could trigger database buffer exhaustion and out-of-memory errors on the JVM.

### 4.3. Rate Limiting Coverage
- **Finding**: Rate limiting is currently implemented only on Groq AI endpoints (`Bucket4j` in-memory filter). Public search, product discovery, login, register, and password-reset endpoints do not have IP-based or client-based rate limiting, leaving them susceptible to scraping, enumeration, and brute-force attacks.

---

## 5. Part IV — Test Coverage Gaps (114 Uncovered Endpoints)

- **Audited State**: 172 endpoints (60.1%) are directly exercised by 20 integration test suites.
- **Gap**: 114 endpoints (39.9%) lack direct MockMvc / integration test coverage.
- **Primary Domains Requiring Test Suites**:
  1. `AdminSellerController` (KYC approval, rejection, seller suspension) — 8 endpoints.
  2. `SellerDocumentController` & `SellerBankController` — 6 endpoints.
  3. `AdminDeliveryController` (fleet oversight, agent assignment) — 7 endpoints.
  4. `InternalStockReservationController` — 4 endpoints.
  5. `CustomerDeliveryTrackingController` — 4 endpoints.
  6. Advanced query/filter parameters across Admin Order and Admin Inventory controllers.

---

## 6. Summary of Gap Severity

| Severity Level | Domain / Issue | Recommended Action Phase |
| :---: | :--- | :--- |
| **CRITICAL** | Seller Payouts & Commission Settlement | Implement Phase 17 (Commission & Finance Engine) |
| **CRITICAL** | Promotions & Coupon Engine | Implement Phase 16 (Promotions & Discounts) |
| **HIGH** | Returns & Reverse Logistics Workflow | Implement Phase 18 (Returns & Replacement) |
| **HIGH** | Indian GST Compliance & Tax Invoicing | Implement Phase 20 (Taxation & Invoicing) |
| **MEDIUM** | Centralized Platform Administration & Audit | Implement Phase 19 (Platform Admin & Audit Log) |
| **MEDIUM** | Bulk CSV Operations & Data Pipelines | Implement Phase 21 (Bulk Catalog & Operations) |
| **LOW** | Minor CRUD additions (Wishlist clear, CSAT rating) | Implement as domain enhancements |
