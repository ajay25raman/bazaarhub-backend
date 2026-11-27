# BazaarHub — Module-Wise API Count & Baseline Comparison

> **Document Status**: Production Complete | **Audit Type**: Architectural Inventory & Module Analysis  
> **Target Project**: BazaarHub Enterprise Multi-Vendor E-Commerce Backend  
> **Total Modules Cataloged**: 28 Modular Domains | **Total Endpoints**: 286 Endpoints  

---

## 1. Executive Summary

The BazaarHub backend is architected as a modular monolith structured around 28 active business domains. Each domain exposes cohesive REST controllers adhering to Clean Architecture principles.

This report provides a granular breakdown of endpoints per module, comparing the audited counts against the **286 endpoint baseline**.

## 2. Master Module API Breakdown Table

| # | Domain Module | Controllers | Total APIs | GET | POST | PUT | PATCH | DEL | Protected | Public | Baseline Match | % of Total |
| :-: | :--- | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| 1 | **Customer Support & Ticketing** | 4 | **30** | 13 | 11 | 0 | 6 | 0 | 30 | 0 | 100% | 10.5% |
| 2 | **Delivery Agent Fleet & Tracking** | 3 | **29** | 13 | 12 | 1 | 3 | 0 | 29 | 0 | 100% | 10.1% |
| 3 | **Seller Onboarding & KYC Management** | 5 | **19** | 9 | 8 | 2 | 0 | 0 | 19 | 0 | 100% | 6.6% |
| 4 | **Notification System** | 3 | **18** | 9 | 2 | 3 | 4 | 0 | 18 | 0 | 100% | 6.3% |
| 5 | **Seller Product & Catalog Management** | 3 | **17** | 5 | 3 | 2 | 4 | 3 | 17 | 0 | 100% | 5.9% |
| 6 | **Shipping & Logistics Management** | 5 | **16** | 12 | 4 | 0 | 0 | 0 | 14 | 2 | 100% | 5.6% |
| 7 | **Inventory Management** | 2 | **15** | 9 | 4 | 0 | 2 | 0 | 15 | 0 | 100% | 5.2% |
| 8 | **Analytics & Reporting** | 3 | **14** | 13 | 1 | 0 | 0 | 0 | 14 | 0 | 100% | 4.9% |
| 9 | **Product Reviews & Ratings** | 4 | **12** | 7 | 1 | 1 | 1 | 2 | 10 | 2 | 100% | 4.2% |
| 10 | **AI Assistant & Intelligence** | 4 | **10** | 6 | 4 | 0 | 0 | 0 | 9 | 1 | 100% | 3.5% |
| 11 | **Category Management** | 2 | **10** | 7 | 1 | 1 | 1 | 0 | 5 | 5 | 100% | 3.5% |
| 12 | **Brand Management** | 2 | **9** | 6 | 1 | 1 | 1 | 0 | 5 | 4 | 100% | 3.1% |
| 13 | **Customer Payment Transactions** | 1 | **8** | 5 | 3 | 0 | 0 | 0 | 8 | 0 | 100% | 2.8% |
| 14 | **Product Catalog (Public/Admin)** | 2 | **8** | 5 | 2 | 0 | 1 | 0 | 5 | 3 | 100% | 2.8% |
| 15 | **Customer Address Management** | 1 | **7** | 3 | 1 | 1 | 1 | 1 | 7 | 0 | 100% | 2.4% |
| 16 | **Customer Profile Management** | 1 | **7** | 2 | 1 | 1 | 3 | 0 | 7 | 0 | 100% | 2.4% |
| 17 | **Customer Wishlist Management** | 1 | **7** | 3 | 1 | 1 | 0 | 2 | 7 | 0 | 100% | 2.4% |
| 18 | **Identity & Authentication (RBAC)** | 1 | **7** | 1 | 6 | 0 | 0 | 0 | 2 | 5 | 100% | 2.4% |
| 19 | **Search & Product Discovery** | 2 | **6** | 6 | 0 | 0 | 0 | 0 | 0 | 6 | 100% | 2.1% |
| 20 | **Shopping Cart Management** | 1 | **6** | 2 | 1 | 1 | 0 | 2 | 6 | 0 | 100% | 2.1% |
| 21 | **Stock Reservation & Availability** | 2 | **6** | 1 | 5 | 0 | 0 | 0 | 5 | 1 | 100% | 2.1% |
| 22 | **Admin Order Operations** | 1 | **5** | 4 | 0 | 0 | 1 | 0 | 5 | 0 | 100% | 1.7% |
| 23 | **Admin Payments & Refunds** | 1 | **5** | 3 | 2 | 0 | 0 | 0 | 5 | 0 | 100% | 1.7% |
| 24 | **Customer Order Management** | 1 | **5** | 3 | 2 | 0 | 0 | 0 | 5 | 0 | 100% | 1.7% |
| 25 | **Seller Order Management** | 1 | **5** | 3 | 1 | 0 | 1 | 0 | 5 | 0 | 100% | 1.7% |
| 26 | **Checkout & Order Placement** | 1 | **3** | 1 | 2 | 0 | 0 | 0 | 3 | 0 | 100% | 1.0% |
| 27 | **Payment Gateway Webhooks** | 1 | **1** | 0 | 1 | 0 | 0 | 0 | 0 | 1 | 100% | 0.3% |
| 28 | **Platform Health & Diagnostics** | 1 | **1** | 1 | 0 | 0 | 0 | 0 | 0 | 1 | 100% | 0.3% |
| :-: | :--- | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| **--** | **TOTAL (28 MODULES)** | **59** | **286** | **152** | **80** | **15** | **29** | **10** | **255** | **31** | **286 / 286 (100%)** | **100.0%** |

---

## 3. Detailed Module-Wise Analysis

### 3.1. Customer Support & Ticketing

- **Controller Classes (4)**: `AdminSupportController`, `CustomerSupportController`, `DeliverySupportController`, `SellerSupportController`
- **Total Endpoints**: **30** (10.5% of backend)
- **HTTP Method Breakdown**: GET: 13, PATCH: 6, POST: 11
- **Security Posture**: 30 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/support/tickets` | Query all support tickets | Yes | `hasAuthority('ROLE_ADMIN')` |
| **POST** | `/api/v1/admin/support/tickets/attachments` | Upload staff attachment | Yes | `hasAuthority('ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/support/tickets/stats` | Get ticket lifecycle metrics | Yes | `hasAuthority('ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/support/tickets/{id}` | Get complete ticket details | Yes | `hasAuthority('ROLE_ADMIN')` |
| **PATCH** | `/api/v1/admin/support/tickets/{id}/assign` | Assign ticket to support agent | Yes | `hasAuthority('ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/support/tickets/{id}/messages` | Get all ticket messages (including internal notes) | Yes | `hasAuthority('ROLE_ADMIN')` |
| **POST** | `/api/v1/admin/support/tickets/{id}/messages` | Post agent reply or private internal note | Yes | `hasAuthority('ROLE_ADMIN')` |
| **PATCH** | `/api/v1/admin/support/tickets/{id}/priority` | Update ticket priority | Yes | `hasAuthority('ROLE_ADMIN')` |
| **PATCH** | `/api/v1/admin/support/tickets/{id}/status` | Update ticket lifecycle status | Yes | `hasAuthority('ROLE_ADMIN')` |
| **GET** | `/api/v1/customer/tickets` | List customer tickets | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/customer/tickets` | Submit support ticket | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/customer/tickets/attachments` | Upload support attachment | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/customer/tickets/{id}` | Get ticket details | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/customer/tickets/{id}/close` | Close ticket | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/customer/tickets/{id}/messages` | Get ticket messages | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/customer/tickets/{id}/messages` | Reply to ticket | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/delivery/tickets` | List delivery partner tickets | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/tickets` | Submit delivery partner support ticket | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/tickets/attachments` | Upload delivery obstacle proof | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/delivery/tickets/{id}` | Get delivery ticket details | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/delivery/tickets/{id}/close` | Close delivery ticket | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/delivery/tickets/{id}/messages` | Get delivery ticket messages | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/tickets/{id}/messages` | Reply to delivery ticket | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/seller/tickets` | List seller tickets | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/seller/tickets` | Submit merchant support ticket | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/seller/tickets/attachments` | Upload seller attachment | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/seller/tickets/{id}` | Get seller ticket details | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/seller/tickets/{id}/close` | Close seller ticket | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/seller/tickets/{id}/messages` | Get seller ticket messages | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/seller/tickets/{id}/messages` | Reply to seller ticket | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |

### 3.2. Delivery Agent Fleet & Tracking

- **Controller Classes (3)**: `AdminDeliveryController`, `CustomerDeliveryTrackingController`, `DeliveryBoyController`
- **Total Endpoints**: **29** (10.1% of backend)
- **HTTP Method Breakdown**: GET: 13, PATCH: 3, POST: 12, PUT: 1
- **Security Posture**: 29 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/delivery/assignments` | List all assignments | Yes | `hasRole('ADMIN')` |
| **POST** | `/api/v1/admin/delivery/assignments` | Assign delivery partner to shipment | Yes | `hasRole('ADMIN')` |
| **POST** | `/api/v1/admin/delivery/assignments/{id}/reassign` | Reassign delivery partner | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/delivery/boys` | List delivery partners | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/delivery/boys/{id}` | Get delivery partner details | Yes | `hasRole('ADMIN')` |
| **PATCH** | `/api/v1/admin/delivery/boys/{id}/status` | Update delivery partner status | Yes | `hasRole('ADMIN')` |
| **PATCH** | `/api/v1/admin/delivery/boys/{id}/verify` | Verify delivery partner KYC | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/delivery/shipments` | List shipments | Yes | `hasRole('ADMIN')` |
| **POST** | `/api/v1/admin/delivery/shipments` | Create shipment for seller sub-order | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/delivery/shipments/{id}` | Get shipment details | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/delivery/tracking/{shipmentId}` | Get shipment tracking history | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/delivery/assignments` | List assigned deliveries | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/delivery/assignments/{id}` | Get delivery assignment details | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/assignments/{id}/accept` | Accept delivery assignment | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/assignments/{id}/complete` | Complete delivery with OTP | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/assignments/{id}/fail` | Report delivery failure | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/assignments/{id}/out-for-delivery` | Mark out for delivery | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/assignments/{id}/pickup` | Confirm parcel pickup | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/assignments/{id}/reject` | Reject delivery assignment | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/delivery/availability` | Toggle availability status | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/onboard` | Onboard as delivery partner | Yes | `Authenticated` |
| **GET** | `/api/v1/delivery/profile` | Get delivery partner profile | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **PUT** | `/api/v1/delivery/profile` | Update delivery partner profile | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/proof/upload` | Upload delivery proof image | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/delivery/route-estimate/{shipmentId}` | Get route distance and duration estimate | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/delivery/tracking/location` | Update live GPS location | Yes | `hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/delivery/tracking/shipments/my` | Get my package shipments | Yes | `hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER',...` |
| **GET** | `/api/v1/delivery/tracking/{shipmentId}` | Get latest live location of shipment | Yes | `hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER',...` |
| **GET** | `/api/v1/delivery/tracking/{shipmentId}/estimate` | Get estimated time of arrival | Yes | `hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER',...` |

### 3.3. Seller Onboarding & KYC Management

- **Controller Classes (5)**: `AdminSellerController`, `SellerBankController`, `SellerController`, `SellerDocumentController`, `StoreController`
- **Total Endpoints**: **19** (6.6% of backend)
- **HTTP Method Breakdown**: GET: 9, POST: 8, PUT: 2
- **Security Posture**: 19 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/sellers` | List seller applications with filtering and pagination | Yes | `hasAuthority('SELLER_MANAGE') or hasRole('ADMI...` |
| **GET** | `/api/v1/admin/sellers/{sellerId}` | Get full seller application dossier | Yes | `hasAuthority('SELLER_MANAGE') or hasRole('ADMI...` |
| **POST** | `/api/v1/admin/sellers/{sellerId}/approve` | Approve seller application | Yes | `hasAuthority('SELLER_MANAGE') or hasRole('ADMI...` |
| **POST** | `/api/v1/admin/sellers/{sellerId}/reject` | Reject seller application | Yes | `hasAuthority('SELLER_MANAGE') or hasRole('ADMI...` |
| **POST** | `/api/v1/admin/sellers/{sellerId}/suspend` | Suspend seller account and storefront | Yes | `hasAuthority('SELLER_MANAGE') or hasRole('ADMI...` |
| **POST** | `/api/v1/admin/sellers/{sellerId}/verify-documents` | Review and verify KYC document | Yes | `hasAuthority('SELLER_MANAGE') or hasRole('ADMI...` |
| **GET** | `/api/v1/sellers/me` | Get current seller profile | Yes | `isAuthenticated()` |
| **PUT** | `/api/v1/sellers/me` | Update seller business profile | Yes | `isAuthenticated()` |
| **GET** | `/api/v1/sellers/me/bank` | List seller bank accounts | Yes | `isAuthenticated()` |
| **POST** | `/api/v1/sellers/me/bank` | Submit settlement bank account | Yes | `isAuthenticated()` |
| **GET** | `/api/v1/sellers/me/bank/{bankId}` | Get seller bank account by ID | Yes | `isAuthenticated()` |
| **GET** | `/api/v1/sellers/me/documents` | List seller KYC documents | Yes | `isAuthenticated()` |
| **POST** | `/api/v1/sellers/me/documents` | Submit KYC verification document | Yes | `isAuthenticated()` |
| **GET** | `/api/v1/sellers/me/documents/{documentId}` | Get seller KYC document by ID | Yes | `isAuthenticated()` |
| **GET** | `/api/v1/sellers/me/status` | Get seller onboarding lifecycle status | Yes | `isAuthenticated()` |
| **GET** | `/api/v1/sellers/me/store` | Get merchant storefront | Yes | `isAuthenticated()` |
| **POST** | `/api/v1/sellers/me/store` | Create merchant storefront | Yes | `isAuthenticated()` |
| **PUT** | `/api/v1/sellers/me/store` | Update merchant storefront | Yes | `isAuthenticated()` |
| **POST** | `/api/v1/sellers/onboarding` | Submit seller onboarding application | Yes | `isAuthenticated()` |

### 3.4. Notification System

- **Controller Classes (3)**: `AdminNotificationController`, `CustomerNotificationController`, `SellerNotificationController`
- **Total Endpoints**: **18** (6.3% of backend)
- **HTTP Method Breakdown**: GET: 9, PATCH: 4, POST: 2, PUT: 3
- **Security Posture**: 18 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/notifications` | Query all notifications | Yes | `hasAuthority('ROLE_ADMIN')` |
| **POST** | `/api/v1/admin/notifications/send` | Send or test notification | Yes | `hasAuthority('ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/notifications/templates` | List notification templates | Yes | `hasAuthority('ROLE_ADMIN')` |
| **POST** | `/api/v1/admin/notifications/templates` | Create notification template | Yes | `hasAuthority('ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/notifications/templates/{id}` | Get notification template by ID | Yes | `hasAuthority('ROLE_ADMIN')` |
| **PUT** | `/api/v1/admin/notifications/templates/{id}` | Update notification template | Yes | `hasAuthority('ROLE_ADMIN')` |
| **GET** | `/api/v1/customer/notification-preferences` | Get customer notification preferences | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **PUT** | `/api/v1/customer/notification-preferences` | Update customer notification preferences | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/customer/notifications` | List customer notifications | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/customer/notifications/read-all` | Mark all notifications as read | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/customer/notifications/unread-count` | Get unread notification count | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/customer/notifications/{id}/read` | Mark notification as read | Yes | `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/seller/notification-preferences` | Get seller notification preferences | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **PUT** | `/api/v1/seller/notification-preferences` | Update seller notification preferences | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/seller/notifications` | List seller notifications | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/seller/notifications/read-all` | Mark all seller notifications as read | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/seller/notifications/unread-count` | Get seller unread notification count | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/seller/notifications/{id}/read` | Mark seller notification as read | Yes | `hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')` |

### 3.5. Seller Product & Catalog Management

- **Controller Classes (3)**: `SellerProductController`, `SellerProductImageController`, `SellerProductVariantController`
- **Total Endpoints**: **17** (5.9% of backend)
- **HTTP Method Breakdown**: DELETE: 3, GET: 5, PATCH: 4, POST: 3, PUT: 2
- **Security Posture**: 17 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/seller/products` | List seller's products | Yes | `hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPD...` |
| **POST** | `/api/v1/seller/products` | Create product | Yes | `hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPD...` |
| **DELETE** | `/api/v1/seller/products/{productId}` | Archive product | Yes | `hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPD...` |
| **GET** | `/api/v1/seller/products/{productId}` | Get seller product by ID | Yes | `hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPD...` |
| **PUT** | `/api/v1/seller/products/{productId}` | Update product details | Yes | `hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPD...` |
| **GET** | `/api/v1/seller/products/{productId}/images` | List product images | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **POST** | `/api/v1/seller/products/{productId}/images` | Add image to product | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **DELETE** | `/api/v1/seller/products/{productId}/images/{imageId}` | Delete product image | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **PATCH** | `/api/v1/seller/products/{productId}/images/{imageId}/order` | Update image display order | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **PATCH** | `/api/v1/seller/products/{productId}/images/{imageId}/primary` | Set primary hero image | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **PATCH** | `/api/v1/seller/products/{productId}/status` | Update product status | Yes | `hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPD...` |
| **GET** | `/api/v1/seller/products/{productId}/variants` | List product variants | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **POST** | `/api/v1/seller/products/{productId}/variants` | Create product variant | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **DELETE** | `/api/v1/seller/products/{productId}/variants/{variantId}` | Delete product variant | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **GET** | `/api/v1/seller/products/{productId}/variants/{variantId}` | Get variant by ID | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **PUT** | `/api/v1/seller/products/{productId}/variants/{variantId}` | Update product variant | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |
| **PATCH** | `/api/v1/seller/products/{productId}/variants/{variantId}/status` | Toggle variant status | Yes | `hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')` |

### 3.6. Shipping & Logistics Management

- **Controller Classes (5)**: `AdminShipmentController`, `CustomerShipmentController`, `InternalShipmentController`, `SellerShipmentController`, `ShipmentTrackingController`
- **Total Endpoints**: **16** (5.6% of backend)
- **HTTP Method Breakdown**: GET: 12, POST: 4
- **Security Posture**: 14 Authenticated/RBAC-Guarded, 2 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/shipments` | List all platform shipments | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/shipments/active` | List active in-flight shipments | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/shipments/failed` | List failed shipments | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/shipments/{shipmentId}` | Get shipment details | Yes | `hasRole('ADMIN')` |
| **POST** | `/api/v1/admin/shipments/{shipmentId}/cancel` | Cancel shipment | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/customer/shipments` | List customer shipments | Yes | `hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER',...` |
| **GET** | `/api/v1/customer/shipments/{shipmentId}` | Get customer shipment details | Yes | `hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER',...` |
| **POST** | `/api/v1/shipments` | Create shipment for seller sub-order | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **GET** | `/api/v1/shipments/order/{orderId}` | Get shipments for customer order | Yes | `hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER',...` |
| **POST** | `/api/v1/shipments/rate-quote` | Calculate shipping rate quote | No | `permitAll()` |
| **GET** | `/api/v1/shipments/seller` | List seller shipments | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **GET** | `/api/v1/shipments/seller/{shipmentId}` | Get seller shipment details | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **GET** | `/api/v1/shipments/track/{trackingNumber}` | Public shipment tracking lookup | No | `permitAll()` |
| **GET** | `/api/v1/shipments/{shipmentId}` | Get shipment details by ID | Yes | `hasAnyAuthority('ORDER_READ', 'SELLER_ORDER_MA...` |
| **POST** | `/api/v1/shipments/{shipmentId}/ready-for-pickup` | Mark parcel ready for pickup | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **GET** | `/api/v1/shipments/{shipmentId}/tracking-history` | Get full tracking history | Yes | `hasAnyAuthority('ORDER_READ', 'SELLER_ORDER_MA...` |

### 3.7. Inventory Management

- **Controller Classes (2)**: `AdminInventoryController`, `SellerInventoryController`
- **Total Endpoints**: **15** (5.2% of backend)
- **HTTP Method Breakdown**: GET: 9, PATCH: 2, POST: 4
- **Security Posture**: 15 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/inventory` | List all platform inventory | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/inventory/low-stock` | List low-stock inventory | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/inventory/out-of-stock` | List out-of-stock inventory | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/inventory/reservations` | List stock reservations | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/inventory/{variantId}` | Get inventory by variant ID | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/admin/inventory/{variantId}/adjustment` | Authoritative stock adjustment | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/inventory/{variantId}/movements` | View variant movement history | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/admin/inventory/{variantId}/safety-stock` | Update safety stock threshold | Yes | `hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/seller/inventory` | List seller inventory | Yes | `hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY...` |
| **GET** | `/api/v1/seller/inventory/{variantId}` | Get variant inventory | Yes | `hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY...` |
| **POST** | `/api/v1/seller/inventory/{variantId}/adjustment` | Physical stock adjustment | Yes | `hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY...` |
| **GET** | `/api/v1/seller/inventory/{variantId}/movements` | View variant movement history | Yes | `hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY...` |
| **PATCH** | `/api/v1/seller/inventory/{variantId}/safety-stock` | Update safety stock threshold | Yes | `hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY...` |
| **POST** | `/api/v1/seller/inventory/{variantId}/stock-in` | Stock replenishment (Stock In) | Yes | `hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY...` |
| **POST** | `/api/v1/seller/inventory/{variantId}/stock-out` | Stock reduction (Stock Out) | Yes | `hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY...` |

### 3.8. Analytics & Reporting

- **Controller Classes (3)**: `AdminAnalyticsController`, `PublicAnalyticsEventController`, `SellerAnalyticsController`
- **Total Endpoints**: **14** (4.9% of backend)
- **HTTP Method Breakdown**: GET: 13, POST: 1
- **Security Posture**: 14 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/analytics/categories` | Category sales performance | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/customers` | Customer cohort metrics and retention | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/delivery` | Logistics and shipping performance | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/orders` | Order fulfillment and volume breakdown | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/overview` | Platform-level high-level executive overview | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/products` | Catalog product performance and inventory health | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/sales` | Sales metrics and daily revenue time series | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/sellers` | Top performing sellers leaderboard | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/analytics/support` | Customer support workload and resolution efficiency | Yes | `hasRole('ADMIN')` |
| **POST** | `/api/v1/public/analytics/events` | Track analytics event | Yes | `Authenticated` |
| **GET** | `/api/v1/seller/analytics/orders` | Seller order status breakdown and fulfillment rate | Yes | `hasAnyRole('SELLER', 'ADMIN')` |
| **GET** | `/api/v1/seller/analytics/overview` | Seller dashboard executive overview | Yes | `hasAnyRole('SELLER', 'ADMIN')` |
| **GET** | `/api/v1/seller/analytics/products` | Seller top performing products | Yes | `hasAnyRole('SELLER', 'ADMIN')` |
| **GET** | `/api/v1/seller/analytics/sales` | Seller sales performance and daily revenue | Yes | `hasAnyRole('SELLER', 'ADMIN')` |

### 3.9. Product Reviews & Ratings

- **Controller Classes (4)**: `AdminReviewController`, `CustomerReviewController`, `PublicReviewController`, `SellerReviewController`
- **Total Endpoints**: **12** (4.2% of backend)
- **HTTP Method Breakdown**: DELETE: 2, GET: 7, PATCH: 1, POST: 1, PUT: 1
- **Security Posture**: 10 Authenticated/RBAC-Guarded, 2 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/reviews` | Get all reviews | Yes | `hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')` |
| **DELETE** | `/api/v1/admin/reviews/{id}` | Delete review | Yes | `hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/reviews/{id}` | Get review details | Yes | `hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/admin/reviews/{id}/moderate` | Moderate product review | Yes | `hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/customer/reviews` | Get customer reviews | Yes | `hasAnyAuthority('REVIEW_READ', 'ROLE_CUSTOMER')` |
| **POST** | `/api/v1/customer/reviews` | Create product review | Yes | `hasAnyAuthority('REVIEW_CREATE', 'ROLE_CUSTOME...` |
| **DELETE** | `/api/v1/customer/reviews/{id}` | Delete product review | Yes | `hasAnyAuthority('REVIEW_DELETE', 'ROLE_CUSTOME...` |
| **GET** | `/api/v1/customer/reviews/{id}` | Get review by ID | Yes | `hasAnyAuthority('REVIEW_READ', 'ROLE_CUSTOMER')` |
| **PUT** | `/api/v1/customer/reviews/{id}` | Update product review | Yes | `hasAnyAuthority('REVIEW_UPDATE', 'ROLE_CUSTOME...` |
| **GET** | `/api/v1/products/{productId}/rating-summary` | Get product rating summary | No | `permitAll()` |
| **GET** | `/api/v1/products/{productId}/reviews` | Get public product reviews | No | `permitAll()` |
| **GET** | `/api/v1/seller/reviews` | Get seller product reviews | Yes | `hasAnyAuthority('REVIEW_READ', 'SELLER_REVIEW_...` |

### 3.10. AI Assistant & Intelligence

- **Controller Classes (4)**: `AdminAiController`, `CustomerAiAssistantController`, `PublicAiController`, `SellerAiCatalogController`
- **Total Endpoints**: **10** (3.5% of backend)
- **HTTP Method Breakdown**: GET: 6, POST: 4
- **Security Posture**: 9 Authenticated/RBAC-Guarded, 1 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **POST** | `/api/v1/admin/ai/generate-description` | Admin catalog description generator | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/ai/status` | Check AI operational health and provider settings | Yes | `hasRole('ADMIN')` |
| **GET** | `/api/v1/admin/ai/usage-statistics` | Retrieve AI token usage and estimated cost analytics | Yes | `hasRole('ADMIN')` |
| **POST** | `/api/v1/customer/ai/chat` | Chat with BazaarHub AI Shopping & Support Assistant | Yes | `hasAnyRole('CUSTOMER', 'ADMIN')` |
| **GET** | `/api/v1/customer/ai/conversations` | List customer's AI conversation history | Yes | `hasAnyRole('CUSTOMER', 'ADMIN')` |
| **GET** | `/api/v1/customer/ai/conversations/{id}/messages` | Retrieve messages in a conversation session | Yes | `hasAnyRole('CUSTOMER', 'ADMIN')` |
| **POST** | `/api/v1/public/ai/chat` | Guest inquiry chat with BazaarHub AI Assistant | Yes | `Authenticated` |
| **GET** | `/api/v1/public/ai/products/{productId}/review-summary` | AI product review sentiment synthesis | No | `permitAll()` |
| **GET** | `/api/v1/public/ai/recommendations` | AI-powered product recommendations | Yes | `Authenticated` |
| **POST** | `/api/v1/seller/ai/generate-description` | Generate AI product titles, descriptions, and SEO tags | Yes | `hasAnyRole('SELLER', 'ADMIN')` |

### 3.11. Category Management

- **Controller Classes (2)**: `AdminCategoryController`, `CategoryController`
- **Total Endpoints**: **10** (3.5% of backend)
- **HTTP Method Breakdown**: GET: 7, PATCH: 1, POST: 1, PUT: 1
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 5 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/categories` | List categories | Yes | `hasAnyAuthority('CATEGORY_MANAGE', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/admin/categories` | Create category | Yes | `hasAnyAuthority('CATEGORY_MANAGE', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/categories/{id}` | Get category by ID | Yes | `hasAnyAuthority('CATEGORY_MANAGE', 'ROLE_ADMIN')` |
| **PUT** | `/api/v1/admin/categories/{id}` | Update category | Yes | `hasAnyAuthority('CATEGORY_MANAGE', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/admin/categories/{id}/status` | Activate or deactivate category | Yes | `hasAnyAuthority('CATEGORY_MANAGE', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/categories` | List active categories | No | `permitAll()` |
| **GET** | `/api/v1/categories/root` | Get root categories | No | `permitAll()` |
| **GET** | `/api/v1/categories/slug/{slug}` | Get active category by slug | No | `permitAll()` |
| **GET** | `/api/v1/categories/tree` | Get category hierarchy | No | `permitAll()` |
| **GET** | `/api/v1/categories/{id}` | Get active category by ID | No | `permitAll()` |

### 3.12. Brand Management

- **Controller Classes (2)**: `AdminBrandController`, `BrandController`
- **Total Endpoints**: **9** (3.1% of backend)
- **HTTP Method Breakdown**: GET: 6, PATCH: 1, POST: 1, PUT: 1
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 4 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/brands` | List brands | Yes | `hasAnyAuthority('BRAND_MANAGE', 'ROLE_ADMIN')` |
| **POST** | `/api/v1/admin/brands` | Create brand | Yes | `hasAnyAuthority('BRAND_MANAGE', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/admin/brands/{id}` | Get brand by ID | Yes | `hasAnyAuthority('BRAND_MANAGE', 'ROLE_ADMIN')` |
| **PUT** | `/api/v1/admin/brands/{id}` | Update brand | Yes | `hasAnyAuthority('BRAND_MANAGE', 'ROLE_ADMIN')` |
| **PATCH** | `/api/v1/admin/brands/{id}/status` | Activate or deactivate brand | Yes | `hasAnyAuthority('BRAND_MANAGE', 'ROLE_ADMIN')` |
| **GET** | `/api/v1/brands` | List active brands | No | `permitAll()` |
| **GET** | `/api/v1/brands/all` | Get all active brands | No | `permitAll()` |
| **GET** | `/api/v1/brands/slug/{slug}` | Get active brand by slug | No | `permitAll()` |
| **GET** | `/api/v1/brands/{id}` | Get active brand by ID | No | `permitAll()` |

### 3.13. Customer Payment Transactions

- **Controller Classes (1)**: `CustomerPaymentController`
- **Total Endpoints**: **8** (2.8% of backend)
- **HTTP Method Breakdown**: GET: 5, POST: 3
- **Security Posture**: 8 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/payments` | List customer payments | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |
| **POST** | `/api/v1/payments/cod/select` | Select Cash on Delivery (COD) | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |
| **GET** | `/api/v1/payments/order/{orderId}` | Get payment details by Order ID | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |
| **POST** | `/api/v1/payments/razorpay/create-order` | Initiate Razorpay checkout order | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |
| **POST** | `/api/v1/payments/razorpay/verify` | Verify Razorpay checkout signature | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |
| **GET** | `/api/v1/payments/refunds` | List customer refunds | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |
| **GET** | `/api/v1/payments/{paymentId}` | Get payment details by ID | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |
| **GET** | `/api/v1/payments/{paymentId}/refunds` | List refunds for a payment | Yes | `hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_REA...` |

### 3.14. Product Catalog (Public/Admin)

- **Controller Classes (2)**: `AdminProductController`, `ProductController`
- **Total Endpoints**: **8** (2.8% of backend)
- **HTTP Method Breakdown**: GET: 5, PATCH: 1, POST: 2
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 3 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/products` | List marketplace products for moderation | Yes | `hasAnyAuthority('PRODUCT_MODERATE', 'ROLE_ADMI...` |
| **GET** | `/api/v1/admin/products/{productId}` | Get product review details | Yes | `hasAnyAuthority('PRODUCT_MODERATE', 'ROLE_ADMI...` |
| **POST** | `/api/v1/admin/products/{productId}/approve` | Approve product | Yes | `hasAnyAuthority('PRODUCT_MODERATE', 'ROLE_ADMI...` |
| **POST** | `/api/v1/admin/products/{productId}/reject` | Reject product | Yes | `hasAnyAuthority('PRODUCT_MODERATE', 'ROLE_ADMI...` |
| **PATCH** | `/api/v1/admin/products/{productId}/status` | Update product status as admin | Yes | `hasAnyAuthority('PRODUCT_MODERATE', 'ROLE_ADMI...` |
| **GET** | `/api/v1/products` | Search public catalog products | No | `permitAll()` |
| **GET** | `/api/v1/products/slug/{slug}` | Get product details by slug | No | `permitAll()` |
| **GET** | `/api/v1/products/{productId}` | Get product details by ID | No | `permitAll()` |

### 3.15. Customer Address Management

- **Controller Classes (1)**: `CustomerAddressController`
- **Total Endpoints**: **7** (2.4% of backend)
- **HTTP Method Breakdown**: DELETE: 1, GET: 3, PATCH: 1, POST: 1, PUT: 1
- **Security Posture**: 7 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/customer/addresses` | Get all addresses | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **POST** | `/api/v1/customer/addresses` | Add delivery address | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **GET** | `/api/v1/customer/addresses/default` | Get default delivery address | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **DELETE** | `/api/v1/customer/addresses/{id}` | Delete delivery address | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **GET** | `/api/v1/customer/addresses/{id}` | Get address by ID | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **PUT** | `/api/v1/customer/addresses/{id}` | Update delivery address | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **PATCH** | `/api/v1/customer/addresses/{id}/default` | Set address as default | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |

### 3.16. Customer Profile Management

- **Controller Classes (1)**: `CustomerProfileController`
- **Total Endpoints**: **7** (2.4% of backend)
- **HTTP Method Breakdown**: GET: 2, PATCH: 3, POST: 1, PUT: 1
- **Security Posture**: 7 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/customer/profile` | Get current customer profile | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **POST** | `/api/v1/customer/profile` | Create customer profile | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **PUT** | `/api/v1/customer/profile` | Update customer profile | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **GET** | `/api/v1/customer/profile/account` | Get complete customer account profile | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **PATCH** | `/api/v1/customer/profile/dob` | Update customer date of birth | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **PATCH** | `/api/v1/customer/profile/gender` | Update customer gender | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |
| **PATCH** | `/api/v1/customer/profile/image` | Update customer profile image URL | Yes | `hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUST...` |

### 3.17. Customer Wishlist Management

- **Controller Classes (1)**: `CustomerWishlistController`
- **Total Endpoints**: **7** (2.4% of backend)
- **HTTP Method Breakdown**: DELETE: 2, GET: 3, POST: 1, PUT: 1
- **Security Posture**: 7 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/customer/wishlist` | Get customer wishlist | Yes | `hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPD...` |
| **PUT** | `/api/v1/customer/wishlist` | Rename customer wishlist | Yes | `hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPD...` |
| **DELETE** | `/api/v1/customer/wishlist/items` | Clear customer wishlist | Yes | `hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPD...` |
| **GET** | `/api/v1/customer/wishlist/items` | Get wishlist items | Yes | `hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPD...` |
| **POST** | `/api/v1/customer/wishlist/items` | Add product to wishlist | Yes | `hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPD...` |
| **DELETE** | `/api/v1/customer/wishlist/items/{productId}` | Remove product from wishlist | Yes | `hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPD...` |
| **GET** | `/api/v1/customer/wishlist/items/{productId}/exists` | Check if product is in wishlist | Yes | `hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPD...` |

### 3.18. Identity & Authentication (RBAC)

- **Controller Classes (1)**: `AuthController`
- **Total Endpoints**: **7** (2.4% of backend)
- **HTTP Method Breakdown**: GET: 1, POST: 6
- **Security Posture**: 2 Authenticated/RBAC-Guarded, 5 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **POST** | `/api/v1/auth/forgot-password` | Initiate password recovery | No | `permitAll()` |
| **POST** | `/api/v1/auth/login` | Authenticate user credentials | No | `permitAll()` |
| **POST** | `/api/v1/auth/logout` | Logout user and deactivate device session | Yes | `Authenticated` |
| **GET** | `/api/v1/auth/me` | Get current authenticated user profile | Yes | `Authenticated` |
| **POST** | `/api/v1/auth/refresh` | Refresh JWT access token | No | `permitAll()` |
| **POST** | `/api/v1/auth/register` | Register new customer account | No | `permitAll()` |
| **POST** | `/api/v1/auth/reset-password` | Complete password reset | No | `permitAll()` |

### 3.19. Search & Product Discovery

- **Controller Classes (2)**: `ProductDiscoveryController`, `ProductSearchController`
- **Total Endpoints**: **6** (2.1% of backend)
- **HTTP Method Breakdown**: GET: 6
- **Security Posture**: 0 Authenticated/RBAC-Guarded, 6 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/discovery/by-category/{categoryId}` | Get recommended products by category | No | `permitAll()` |
| **GET** | `/api/v1/discovery/featured` | Get featured products | No | `permitAll()` |
| **GET** | `/api/v1/discovery/new-arrivals` | Get new arrival products | No | `permitAll()` |
| **GET** | `/api/v1/discovery/related/{productId}` | Get related products | No | `permitAll()` |
| **GET** | `/api/v1/search/categories/{categoryId}/products` | Browse products by category | No | `permitAll()` |
| **GET** | `/api/v1/search/products` | Search public catalog products | No | `permitAll()` |

### 3.20. Shopping Cart Management

- **Controller Classes (1)**: `CustomerCartController`
- **Total Endpoints**: **6** (2.1% of backend)
- **HTTP Method Breakdown**: DELETE: 2, GET: 2, POST: 1, PUT: 1
- **Security Posture**: 6 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **DELETE** | `/api/v1/cart` | Clear all items from cart | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |
| **GET** | `/api/v1/cart` | Get current customer's cart | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |
| **POST** | `/api/v1/cart/items` | Add item to cart | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |
| **DELETE** | `/api/v1/cart/items/{itemId}` | Remove item from cart | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |
| **PUT** | `/api/v1/cart/items/{itemId}` | Update cart item quantity | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |
| **GET** | `/api/v1/cart/summary` | Get cart summary | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |

### 3.21. Stock Reservation & Availability

- **Controller Classes (2)**: `InternalStockReservationController`, `PublicStockAvailabilityController`
- **Total Endpoints**: **6** (2.1% of backend)
- **HTTP Method Breakdown**: GET: 1, POST: 5
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 1 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **POST** | `/api/v1/internal/inventory/reservations/cancel` | Cancel stock reservation | Yes | `hasAnyAuthority('ROLE_ADMIN', 'INVENTORY_AUDIT')` |
| **POST** | `/api/v1/internal/inventory/reservations/confirm` | Confirm stock reservation | Yes | `hasAnyAuthority('ROLE_ADMIN', 'INVENTORY_AUDIT')` |
| **POST** | `/api/v1/internal/inventory/reservations/release` | Release stock reservation | Yes | `hasAnyAuthority('ROLE_ADMIN', 'INVENTORY_AUDIT')` |
| **POST** | `/api/v1/internal/inventory/reservations/reserve` | Reserve stock for checkout | Yes | `hasAnyAuthority('ROLE_ADMIN', 'INVENTORY_AUDIT')` |
| **POST** | `/api/v1/internal/inventory/reservations/sweep-expired` | Sweep expired reservations | Yes | `hasAnyAuthority('ROLE_ADMIN', 'INVENTORY_AUDIT')` |
| **GET** | `/api/v1/products/{productId}/variants/{variantId}/availability` | Check public stock availability | No | `permitAll()` |

### 3.22. Admin Order Operations

- **Controller Classes (1)**: `AdminOrderController`
- **Total Endpoints**: **5** (1.7% of backend)
- **HTTP Method Breakdown**: GET: 4, PATCH: 1
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/orders` | Search and list all orders | Yes | `hasAnyAuthority('ADMIN_ORDER_MANAGE', 'ROLE_AD...` |
| **GET** | `/api/v1/admin/orders/seller-orders` | List all seller sub-orders | Yes | `hasAnyAuthority('ADMIN_ORDER_MANAGE', 'ROLE_AD...` |
| **GET** | `/api/v1/admin/orders/{orderId}` | Get complete order tree | Yes | `hasAnyAuthority('ADMIN_ORDER_MANAGE', 'ROLE_AD...` |
| **GET** | `/api/v1/admin/orders/{orderId}/history` | Get order audit history | Yes | `hasAnyAuthority('ADMIN_ORDER_MANAGE', 'ROLE_AD...` |
| **PATCH** | `/api/v1/admin/orders/{orderId}/status` | Administratively update order status | Yes | `hasAnyAuthority('ADMIN_ORDER_MANAGE', 'ROLE_AD...` |

### 3.23. Admin Payments & Refunds

- **Controller Classes (1)**: `AdminPaymentController`
- **Total Endpoints**: **5** (1.7% of backend)
- **HTTP Method Breakdown**: GET: 3, POST: 2
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/admin/payments` | List all payments | Yes | `hasAnyAuthority('PAYMENT_MANAGE', 'REFUND_MANA...` |
| **POST** | `/api/v1/admin/payments/{paymentId}/cod-collect` | Collect Cash on Delivery payment | Yes | `hasAnyAuthority('PAYMENT_MANAGE', 'REFUND_MANA...` |
| **POST** | `/api/v1/admin/payments/{paymentId}/refunds` | Initiate payment refund | Yes | `hasAnyAuthority('PAYMENT_MANAGE', 'REFUND_MANA...` |
| **GET** | `/api/v1/admin/refunds` | List all refunds | Yes | `hasAnyAuthority('PAYMENT_MANAGE', 'REFUND_MANA...` |
| **GET** | `/api/v1/admin/refunds/{refundId}` | Get refund details by ID | Yes | `hasAnyAuthority('PAYMENT_MANAGE', 'REFUND_MANA...` |

### 3.24. Customer Order Management

- **Controller Classes (1)**: `CustomerOrderController`
- **Total Endpoints**: **5** (1.7% of backend)
- **HTTP Method Breakdown**: GET: 3, POST: 2
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/orders` | List customer orders | Yes | `hasAnyAuthority('ORDER_CREATE', 'ORDER_READ', ...` |
| **POST** | `/api/v1/orders` | Place order from active cart | Yes | `hasAnyAuthority('ORDER_CREATE', 'ORDER_READ', ...` |
| **GET** | `/api/v1/orders/by-number/{orderNumber}` | Get order details by order number | Yes | `hasAnyAuthority('ORDER_CREATE', 'ORDER_READ', ...` |
| **GET** | `/api/v1/orders/{orderId}` | Get order details by ID | Yes | `hasAnyAuthority('ORDER_CREATE', 'ORDER_READ', ...` |
| **POST** | `/api/v1/orders/{orderId}/cancel` | Cancel an order | Yes | `hasAnyAuthority('ORDER_CREATE', 'ORDER_READ', ...` |

### 3.25. Seller Order Management

- **Controller Classes (1)**: `SellerOrderController`
- **Total Endpoints**: **5** (1.7% of backend)
- **HTTP Method Breakdown**: GET: 3, PATCH: 1, POST: 1
- **Security Posture**: 5 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/seller/orders` | List seller sub-orders | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **GET** | `/api/v1/seller/orders/by-number/{subOrderNumber}` | Get seller sub-order by sub-order number | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **GET** | `/api/v1/seller/orders/{sellerOrderId}` | Get seller sub-order by ID | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **POST** | `/api/v1/seller/orders/{sellerOrderId}/cancel` | Cancel seller sub-order | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |
| **PATCH** | `/api/v1/seller/orders/{sellerOrderId}/status` | Update seller sub-order fulfillment status | Yes | `hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_S...` |

### 3.26. Checkout & Order Placement

- **Controller Classes (1)**: `CustomerCheckoutController`
- **Total Endpoints**: **3** (1.0% of backend)
- **HTTP Method Breakdown**: GET: 1, POST: 2
- **Security Posture**: 3 Authenticated/RBAC-Guarded, 0 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **POST** | `/api/v1/checkout/address` | Select delivery address for checkout | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |
| **GET** | `/api/v1/checkout/summary` | Get checkout summary | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |
| **POST** | `/api/v1/checkout/validate` | Validate checkout readiness | Yes | `hasAnyAuthority('CART_READ', 'CART_UPDATE', 'R...` |

### 3.27. Payment Gateway Webhooks

- **Controller Classes (1)**: `PaymentWebhookController`
- **Total Endpoints**: **1** (0.3% of backend)
- **HTTP Method Breakdown**: POST: 1
- **Security Posture**: 0 Authenticated/RBAC-Guarded, 1 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **POST** | `/api/v1/payments/webhooks/razorpay` | Receive Razorpay webhook event | No | `permitAll()` |

### 3.28. Platform Health & Diagnostics

- **Controller Classes (1)**: `HealthController`
- **Total Endpoints**: **1** (0.3% of backend)
- **HTTP Method Breakdown**: GET: 1
- **Security Posture**: 0 Authenticated/RBAC-Guarded, 1 Public

| Method | Endpoint Path | Functionality / Summary | Auth Required | RBAC / Permission Guard |
| :---: | :--- | :--- | :---: | :--- |
| **GET** | `/api/v1/health` | health | No | `permitAll()` |

