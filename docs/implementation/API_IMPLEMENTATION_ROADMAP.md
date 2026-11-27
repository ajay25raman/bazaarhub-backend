# BazaarHub — API Implementation Roadmap & Prioritization

> **Document Status**: Production Complete | **Audit Type**: Strategic Implementation Roadmap  
> **Target Project**: BazaarHub Enterprise Multi-Vendor E-Commerce Platform  
> **Baseline**: 286 Implemented Endpoints | **Proposed Roadmap**: 58 Strategic Missing APIs  
> **Architecture**: Java 21 | Spring Boot 4.1.2 | MariaDB 11.8.9 | Clean Architecture  

---

## 1. Executive Summary & Strategic Prioritization

Following the exhaustive discovery of BazaarHub's 286 baseline APIs and the identification of architectural gaps, this roadmap establishes a disciplined, prioritized execution plan to bridge functional deficiencies.

### Prioritization Tiers:
- **Tier P0 (Critical Marketplace Blockers)**: Required for fundamental commercial operations. Without these, sellers cannot receive funds and customers cannot redeem promotional offers.
- **Tier P1 (High-Value Commercial Capabilities)**: Required for customer retention, seller dispute resolution, and platform-wide auditability (Returns, Reverse Logistics, Centralized Administration).
- **Tier P2 (Regulatory Compliance & Scale)**: Indian GST compliance, tax invoicing, bulk catalog upload for enterprise vendors.
- **Tier P3 (Advanced Intelligence & Telemetry)**: Live WebSocket fleet tracking, typeahead search autocomplete, review helpfulness voting.

---

## 2. Master Prioritized API Implementation Matrix

The table below details the **58 high-impact missing APIs** identified during the audit, mapped to their target modules, business justifications, schema impacts, and service reuse opportunities.

| # | Method | Proposed API Path | Target Module | Business Purpose | Priority | Justification | Schema Impact | Service / Entity Reuse |
| :-: | :---: | :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | **POST** | `/api/v1/cart/coupons/apply` | Promotions & Discounts | Apply coupon code to active cart | **P0** | Direct conversion driver; core commerce feature | New `coupons`, `coupon_redemptions` tables | Reuses `CustomerCartService` & Cart DTOs |
| 2 | **DELETE** | `/api/v1/cart/coupons/remove` | Promotions & Discounts | Remove applied coupon from cart | **P0** | Allows user to change or remove coupon | Updates active cart session/entity | Reuses `CustomerCartService` |
| 3 | **GET** | `/api/v1/cart/coupons/applicable` | Promotions & Discounts | List eligible coupons for current cart | **P0** | Increases checkout completion and average order value | Reads `coupons` table with criteria | Reuses cart total calculation |
| 4 | **GET** | `/api/v1/promotions/active` | Promotions & Discounts | Public active flash sales & banner promos | **P0** | Homepage marketing banners and deals | New `promotions` table | Integrates with Catalog Service |
| 5 | **POST** | `/api/v1/seller/promotions` | Promotions & Discounts | Seller creates store/product discount | **P0** | Enables seller-funded promotions | New `seller_promotions` table | Reuses `SellerService` & `ProductEntity` |
| 6 | **GET** | `/api/v1/seller/promotions` | Promotions & Discounts | List seller's active & past promotions | **P0** | Seller promotion lifecycle management | Reads `seller_promotions` table | Scoped to authenticated `sellerId` |
| 7 | **POST** | `/api/v1/admin/coupons` | Promotions & Discounts | Admin creates platform-wide coupon | **P0** | Platform marketing campaign management | Writes to `coupons` table | Secured with `ROLE_ADMIN` |
| 8 | **GET** | `/api/v1/admin/coupons` | Promotions & Discounts | List all coupons with usage statistics | **P0** | Audits marketing spend and discount liability | Paged query on `coupons` | Secured with `ROLE_ADMIN` |
| 9 | **PUT** | `/api/v1/admin/coupons/{id}/status` | Promotions & Discounts | Activate/Deactivate coupon code | **P0** | Emergency campaign shutoff control | Updates `status` column in `coupons` | Secured with `ROLE_ADMIN` |
| 10 | **GET** | `/api/v1/seller/payouts` | Commission & Payouts | Seller payout history & balance summary | **P0** | Essential for seller visibility into earnings | New `seller_payouts`, `payout_items` tables | Reuses `SellerOrderEntity.commissionAmount` |
| 11 | **POST** | `/api/v1/seller/payouts/request` | Commission & Payouts | Seller requests withdrawal of mature balance | **P0** | Initiates vendor fund disbursement | Inserts pending record into `seller_payouts` | Reuses `SellerBankEntity` for bank account |
| 12 | **GET** | `/api/v1/seller/payouts/{payoutId}/invoice` | Commission & Payouts | Download payout statement / TDS receipt | **P0** | Tax compliance and accounting for sellers | Reads `seller_payouts` and generates PDF | Reuses iText/Thymeleaf PDF engine |
| 13 | **GET** | `/api/v1/admin/commissions/rules` | Commission & Payouts | List category commission rate rules | **P0** | View current platform fee schedule | New `commission_rules` table | Reuses `CategoryEntity` |
| 14 | **POST** | `/api/v1/admin/commissions/rules` | Commission & Payouts | Configure category-specific commission % | **P0** | Dynamic platform revenue configuration | Writes to `commission_rules` table | Secured with `ROLE_ADMIN` |
| 15 | **GET** | `/api/v1/admin/payouts/pending` | Commission & Payouts | Admin queue of pending payout requests | **P0** | Financial compliance and fraud review | Paged query on `seller_payouts` | Secured with `ROLE_ADMIN` |
| 16 | **POST** | `/api/v1/admin/payouts/{payoutId}/process` | Commission & Payouts | Approve & trigger bank payout transfer | **P0** | Executes fund transfer via Razorpay Payouts | Updates `seller_payouts.status = 'PROCESSED'` | Integrates with Razorpay Payouts API |
| 17 | **POST** | `/api/v1/admin/payouts/{payoutId}/reject` | Commission & Payouts | Reject payout request with audit reason | **P0** | Prevents fraudulent withdrawals | Updates `status = 'REJECTED'`, releases balance | Reuses Notification Service for alert |
| 18 | **GET** | `/api/v1/admin/finance/ledger` | Commission & Payouts | Master platform reconciliation ledger | **P0** | Complete accounting audit trail | New `finance_ledger_entries` table | Aggregates orders, refunds, and payouts |
| 19 | **POST** | `/api/v1/orders/{orderId}/returns` | Returns & Reverse Logistics | Customer initiates return request with reason | **P1** | Protects buyer rights; core e-commerce flow | New `order_returns`, `return_items` tables | Validates against `CustomerOrderEntity` |
| 20 | **GET** | `/api/v1/customer/returns` | Returns & Reverse Logistics | Customer returns history & status tracking | **P1** | Customer self-service tracking | Paged query on `order_returns` | Scoped to `principal.getId()` |
| 21 | **GET** | `/api/v1/customer/returns/{returnId}` | Returns & Reverse Logistics | Detailed return progress & milestones | **P1** | Milestone timeline (Pickup, QC, Refund) | Reads `order_returns` with items | Scoped to `principal.getId()` |
| 22 | **POST** | `/api/v1/customer/returns/{returnId}/cancel` | Returns & Reverse Logistics | Customer cancels return request | **P1** | Self-service cancellation before pickup | Updates `order_returns.status = 'CANCELLED'` | Reuses Notification Service |
| 23 | **GET** | `/api/v1/seller/returns` | Returns & Reverse Logistics | Seller received return requests | **P1** | Seller visibility into defective products | Paged query filtered by `sellerId` | Scoped to authenticated seller |
| 24 | **POST** | `/api/v1/seller/returns/{returnId}/approve` | Returns & Reverse Logistics | Seller authorizes return for pickup | **P1** | Triggers reverse shipment generation | Updates `status = 'APPROVED'` | Integrates with Delivery Service for pickup |
| 25 | **POST** | `/api/v1/seller/returns/{returnId}/qc-complete` | Returns & Reverse Logistics | Seller records warehouse QC outcome | **P1** | Controls whether refund is released or rejected | Updates `qc_status`, triggers refund | Integrates with `CustomerPaymentService` |
| 26 | **GET** | `/api/v1/delivery/returns/assigned` | Returns & Reverse Logistics | Delivery agent assigned return pickups | **P1** | Logistics fleet reverse pickup workflow | Reads `order_returns` assigned to agent | Scoped to `deliveryAgentId` |
| 27 | **POST** | `/api/v1/delivery/returns/{returnId}/pickup-complete` | Returns & Reverse Logistics | Delivery agent confirms pickup with customer OTP | **P1** | Verifies physical handover of returned item | Updates `status = 'PICKED_UP'` | Reuses OTP generator service |
| 28 | **GET** | `/api/v1/admin/returns` | Returns & Reverse Logistics | Platform-wide returns oversight & disputes | **P1** | Dispute resolution and quality monitoring | Paged query on `order_returns` | Secured with `ROLE_ADMIN` |
| 29 | **GET** | `/api/v1/admin/system/settings` | Platform Administration | Retrieve global platform configurations | **P1** | Centrally manage business parameters | New `system_settings` table | Cached in Spring Cache / Redis |
| 30 | **PUT** | `/api/v1/admin/system/settings` | Platform Administration | Update global marketplace settings | **P1** | Allows runtime configuration changes | Updates `system_settings` table | Secured with `ROLE_ADMIN` |
| 31 | **GET** | `/api/v1/admin/audit-logs` | Platform Administration | Query immutable administrative audit logs | **P1** | Compliance, accountability, and forensics | New `audit_logs` table | Spring Security Audit Listener |
| 32 | **GET** | `/api/v1/admin/staff` | Platform Administration | List staff accounts & assigned roles | **P1** | Internal employee management | Reads `users` table where role in admin tier | Reuses `UserEntity` & `RoleEntity` |
| 33 | **POST** | `/api/v1/admin/staff/invite` | Platform Administration | Invite new staff (Support, Moderator, Finance) | **P1** | Role-based delegation of duties | Inserts into `users`, sends invite email | Reuses `NotificationService` |
| 34 | **PUT** | `/api/v1/admin/staff/{id}/roles` | Platform Administration | Update staff role assignments | **P1** | Enforces principle of least privilege | Updates `user_roles` join table | Secured with `ROLE_ADMIN` |
| 35 | **POST** | `/api/v1/admin/system/maintenance-mode` | Platform Administration | Toggle maintenance mode with IP whitelist | **P1** | Allows zero-downtime database upgrades | Updates `system_settings.maintenance_mode` | Checked in Spring Security Filter |
| 36 | **GET** | `/api/v1/tax/hsn-lookup` | Taxation & Invoicing | Search HSN/SAC codes and GST rate slabs | **P2** | Ensures sellers select valid tax rates | New `hsn_tax_slabs` table | Cached reference data |
| 37 | **POST** | `/api/v1/seller/tax/validate-gstin` | Taxation & Invoicing | Verify seller GSTIN format & state code | **P2** | Prevents fraudulent tax filings | External GSTIN API validation | Updates `seller_profiles.gstin_verified` |
| 38 | **GET** | `/api/v1/orders/{orderId}/tax-invoice` | Taxation & Invoicing | Generate compliant GST PDF tax invoice | **P2** | Mandatory legal requirement for Indian commerce | Reads order items, computes CGST/SGST/IGST | Reuses iText/OpenPDF template |
| 39 | **GET** | `/api/v1/seller/reports/gstr1` | Taxation & Invoicing | Export monthly GSTR-1 sales register CSV | **P2** | Enables seller monthly GST return filing | Queries completed orders in month | Streams CSV response |
| 40 | **GET** | `/api/v1/admin/tax/tcs-reports` | Taxation & Invoicing | Export Section 52 1% TCS tax deduction report | **P2** | Marketplace operator statutory tax compliance | Queries seller sales & TCS deductions | Streams CSV report |
| 41 | **POST** | `/api/v1/seller/products/bulk-upload` | Bulk Operations | Asynchronous product creation via CSV | **P2** | Essential for onboarding large enterprise vendors | New `bulk_upload_jobs` table | Dispatches async job via Spring TaskExecutor |
| 42 | **GET** | `/api/v1/seller/products/bulk-upload/{jobId}/status` | Bulk Operations | Poll status & error log of bulk upload job | **P2** | Gives seller feedback on failed CSV rows | Reads `bulk_upload_jobs` | Scoped to `sellerId` |
| 43 | **POST** | `/api/v1/seller/inventory/bulk-update` | Bulk Operations | Batch update stock levels for multiple SKUs | **P2** | Speeds up daily warehouse stock adjustments | Batch update on `inventories` | Reuses `SellerInventoryService` |
| 44 | **GET** | `/api/v1/seller/orders/export` | Bulk Operations | Export seller order records to CSV | **P2** | Integrates with seller external ERP / Tally | Queries `seller_orders` within date range | Streams CSV response |
| 45 | **GET** | `/api/v1/admin/orders/export` | Bulk Operations | Platform-wide order export to CSV | **P2** | Business intelligence & external audits | Queries all orders within date range | Secured with `ROLE_ADMIN` |
| 46 | **GET** | `/api/v1/search/autocomplete` | Search & Discovery | Fast typeahead query completion suggestions | **P3** | Improves mobile search conversion | Reads product search keywords / Redis cache | Reuses `ProductSearchService` |
| 47 | **GET** | `/api/v1/discovery/trending-tags` | Search & Discovery | Dynamic top-searched tags & hashtags | **P3** | Surfaces trending fashion/tech tags | Aggregates search event analytics | Reuses `AnalyticsEventEntity` |
| 48 | **POST** | `/api/v1/customer/wishlist/move-to-cart/{itemId}` | Customer Wishlist | Atomically move wishlist item to cart | **P3** | Reduces friction from wishlist to purchase | Deletes from wishlist, adds to cart | Reuses Cart and Wishlist services |
| 49 | **POST** | `/api/v1/customer/wishlist/clear` | Customer Wishlist | Clear entire customer wishlist | **P3** | Quality of life wishlist management | Bulk delete from `wishlist_items` | Scoped to `principal.getId()` |
| 50 | **POST** | `/api/v1/cart/merge` | Shopping Cart | Merge anonymous guest cart into customer cart | **P3** | Preserves items selected before login | Transfers guest cart items to user cart | Reuses `CustomerCartService` |
| 51 | **POST** | `/api/v1/orders/{orderId}/cancel-reason` | Customer Orders | Capture structured cancellation feedback | **P3** | Enables churn and cancellation analysis | Updates `cancellation_reason_code` | Reuses `CustomerOrderEntity` |
| 52 | **POST** | `/api/v1/customer/reviews/{reviewId}/vote` | Product Reviews | Upvote or downvote review helpfulness | **P3** | Surfaces highest-quality reviews to shoppers | New `review_votes` table | Prevents duplicate voting per user |
| 53 | **POST** | `/api/v1/customer/reviews/{reviewId}/report` | Product Reviews | Flag review for moderation review | **P3** | Community moderation of spam/abuse | Updates review flag counter / moderation queue | Alerts Admin Review queue |
| 54 | **PATCH** | `/api/v1/customer/notifications/mark-all-read` | Notification System | Bulk mark all notifications as read | **P3** | UI notification inbox convenience | Bulk update on `notifications` table | Scoped to `principal.getId()` |
| 55 | **GET** | `/api/v1/customer/notifications/unread-count` | Notification System | Get badge unread notifications count | **P3** | Fast unread badge counter for mobile header | Indexed count query on `notifications` | Scoped to `principal.getId()` |
| 56 | **POST** | `/api/v1/customer/tickets/{id}/rate` | Customer Support | Submit CSAT rating (1-5 stars) upon resolution | **P3** | Measures support agent quality & satisfaction | Updates `satisfaction_rating` in `support_tickets` | Scoped to ticket creator |
| 57 | **POST** | `/api/v1/admin/support/tickets/{id}/escalate` | Customer Support | Escalate ticket priority to Tier 3 / Management | **P3** | Emergency handling for severe customer issues | Updates `priority = 'URGENT'`, reassigns | Secured with `ROLE_ADMIN` |
| 58 | **GET** | `/api/v1/delivery/tracking/stream/{shipmentId}` | Delivery Fleet | SSE / WebSocket stream of live agent GPS coords | **P3** | Uber-style live delivery tracking on customer map | In-memory coordinates cache / WebSocket | Reuses `DeliveryBoyLocation` model |

---

## 3. Phased Implementation Roadmap

### 3.16. Phase 16: Promotions, Coupons & Discount Engine (P0 — 9 APIs)

- **Scope & Core Capabilities**: Coupons lifecycle, percentage/fixed cart discounts, minimum order threshold, atomic redemption lock, seller-funded store discounts, active deal banners.
- **Architecture & Persistence Strategy**: New tables: `coupons`, `coupon_redemptions`, `seller_promotions`. Integrates with Cart & Checkout.
- **Priority Justification**: Governs the commercial and operational viability of BazaarHub as an enterprise multi-vendor platform.

### 3.17. Phase 17: Seller Payouts, Commission Rules & Finance Ledger (P0 — 9 APIs)

- **Scope & Core Capabilities**: Category commission rule tiers, seller balance maturity lifecycle, payout withdrawal requests, Razorpay Payouts bank transfer, double-entry financial ledger.
- **Architecture & Persistence Strategy**: New tables: `commission_rules`, `seller_payouts`, `payout_items`, `finance_ledger_entries`. Integrates with Razorpay Payouts.
- **Priority Justification**: Governs the commercial and operational viability of BazaarHub as an enterprise multi-vendor platform.

### 3.18. Phase 18: Returns, Reverse Logistics & Replacement Workflow (P1 — 10 APIs)

- **Scope & Core Capabilities**: Customer return initiation with photos/reasons, seller authorization, reverse pickup dispatch to delivery fleet, OTP-verified pickup, warehouse QC inspection, refund release.
- **Architecture & Persistence Strategy**: New tables: `order_returns`, `return_items`, `return_timeline`. Integrates with Delivery & Payment.
- **Priority Justification**: Governs the commercial and operational viability of BazaarHub as an enterprise multi-vendor platform.

### 3.19. Phase 19: Centralized Platform Admin, Audit Log & Staff RBAC (P1 — 7 APIs)

- **Scope & Core Capabilities**: Global system configuration settings, immutable administrator audit log stream, internal staff provisioning, maintenance mode switch with IP bypass.
- **Architecture & Persistence Strategy**: New tables: `system_settings`, `audit_logs`. Spring Security Audit Interceptor.
- **Priority Justification**: Governs the commercial and operational viability of BazaarHub as an enterprise multi-vendor platform.

### 3.20. Phase 20: Indian GST Compliance, HSN Mapping & Tax Invoices (P2 — 5 APIs)

- **Scope & Core Capabilities**: HSN/SAC code search, seller GSTIN verification, dynamic CGST+SGST (intra-state) vs IGST (inter-state) split, PDF tax invoice generator with IRN/QR code, monthly GSTR-1 & TCS reports.
- **Architecture & Persistence Strategy**: New tables: `hsn_tax_slabs`. Updates to `seller_profiles`. PDF generation via iText.
- **Priority Justification**: Governs the commercial and operational viability of BazaarHub as an enterprise multi-vendor platform.

### 3.21. Phase 21: Bulk Catalog Operations, CSV Import & Data Pipelines (P2 — 5 APIs)

- **Scope & Core Capabilities**: Asynchronous bulk CSV product creation, background job progress polling, bulk inventory adjustment across multiple SKUs, seller/admin order export to CSV.
- **Architecture & Persistence Strategy**: New tables: `bulk_upload_jobs`. Spring Batch / TaskExecutor async worker.
- **Priority Justification**: Governs the commercial and operational viability of BazaarHub as an enterprise multi-vendor platform.

### 3.22. Phase 22: Advanced Marketplace Intelligence & Real-Time Telemetry (P3 — 13 APIs)

- **Scope & Core Capabilities**: Live delivery agent WebSocket/SSE GPS coordinates stream, typeahead search autocomplete, review helpfulness voting, CSAT support surveys, bulk notification operations.
- **Architecture & Persistence Strategy**: Enhancements to existing tables (`support_tickets`, `notifications`, `reviews`). WebSocket config.
- **Priority Justification**: Governs the commercial and operational viability of BazaarHub as an enterprise multi-vendor platform.

---

## 4. Enterprise Clean Architecture Implementation Guidelines

When implementing any phase from this roadmap, the following engineering rules MUST be strictly followed:

1. **Strict Clean Architecture Layers**:
   - `domain`: Pure business rules, entities, value objects, domain events, repository SPI. Zero framework dependencies.
   - `application`: Use cases, input ports, command/query handlers, application DTOs.
   - `infrastructure`: JPA entities, MariaDB Flyway migrations (`V{Phase}_{Sub}__description.sql`), Spring Data repositories, external SDK adapters.
   - `presentation`: REST controllers, `@Valid` request DTOs, standard `ApiResponse<T>` wrappers, OpenAPI 3 annotations.
2. **Database Migration Safety**:
   - All schema changes must be scripted in idempotent, versioned Flyway migration files.
   - Migrations must be verified against local/staging test containers before applying to Hostinger MariaDB.
   - Hibernate `ddl-auto=validate` must remain strictly enforced.
3. **Security & Ownership Standards**:
   - Both URL-level security rules in `SecurityConfig.java` AND method-level `@PreAuthorize` must be configured.
   - Every customer/seller/delivery endpoint must validate ownership via `@AuthenticationPrincipal UserPrincipal principal`.
4. **Test Verification Mandate**:
   - Every newly implemented controller must be accompanied by comprehensive MockMvc integration tests achieving >85% branch coverage.

---
### Roadmap Approval Status
- **Audit Completion**: All 286 existing APIs cataloged, gaps analyzed, and 58 roadmap APIs prioritized.
- **Next Step**: Awaiting user approval to proceed with Phase 16 (Promotions, Coupons & Discounts).
