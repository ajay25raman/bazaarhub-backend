# BazaarHub Business Modules Catalog

## 1. Purpose
This document catalogs all 20 business modules comprising the BazaarHub Modular Monolith backend. It establishes the domain boundaries, business ownership, and cross-module interaction rules.

---

## 2. Business Modules Overview

| # | Module | Scope & Domain Responsibility | Package Path |
| :---: | :--- | :--- | :--- |
| 1 | `identity` | Authentication, Authorization, RBAC, JWT, MFA, Sessions | [`com.bazaarhub.backend.identity`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/identity/README.md) |
| 2 | `customer` | Buyer profile, address book, preferences, GDPR compliance | [`com.bazaarhub.backend.customer`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/customer/README.md) |
| 3 | `seller` | Vendor onboarding, KYC verification, legal details, badges | [`com.bazaarhub.backend.seller`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/seller/README.md) |
| 4 | `store` | Storefront management, operating hours, geolocation radius | [`com.bazaarhub.backend.store`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/store/README.md) |
| 5 | `catalog` | Products, taxonomies, categories, brands, variants, SKUs | [`com.bazaarhub.backend.catalog`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/catalog/README.md) |
| 6 | `inventory` | Stock tracking, warehouse allocation, checkout reservation | [`com.bazaarhub.backend.inventory`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/inventory/README.md) |
| 7 | `cart` | Active carts (guest/authenticated), item validation, merge | [`com.bazaarhub.backend.cart`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/cart/README.md) |
| 8 | `wishlist` | Saved items, price drop tracking, wishlist sharing | [`com.bazaarhub.backend.wishlist`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/wishlist/README.md) |
| 9 | `order` | Order placement, state machine, sub-orders, order history | [`com.bazaarhub.backend.order`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/order/README.md) |
| 10 | `payment` | Payment intent, gateway adapters (Stripe, etc.), webhooks | [`com.bazaarhub.backend.payment`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/payment/README.md) |
| 11 | `refund` | Refund requests, return authorization, payment reversal | [`com.bazaarhub.backend.refund`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/refund/README.md) |
| 12 | `delivery` | Delivery boy assignment, dispatch, geotracking, POD verification | [`com.bazaarhub.backend.delivery`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/delivery/README.md) |
| 13 | `commission` | Platform fee calculation, seller tier discount, tax on fees | [`com.bazaarhub.backend.commission`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/commission/README.md) |
| 14 | `finance` | Vendor payouts, double-entry ledger, escrow, tax withholding | [`com.bazaarhub.backend.finance`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/finance/README.md) |
| 15 | `promotion` | Coupons, flash sales, discount engine, usage limits | [`com.bazaarhub.backend.promotion`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/promotion/README.md) |
| 16 | `review` | Ratings, product/store reviews, moderation, verified buyer | [`com.bazaarhub.backend.review`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/review/README.md) |
| 17 | `notification` | Push notifications (FCM), SMS, email, in-app inbox | [`com.bazaarhub.backend.notification`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/notification/README.md) |
| 18 | `support` | Helpdesk ticketing, buyer-seller disputes, resolution | [`com.bazaarhub.backend.support`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/README.md) |
| 19 | `admin` | Platform governance, analytics, moderation, audit review | [`com.bazaarhub.backend.admin`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/admin/README.md) |
| 20 | `ai` | Recommendations, semantic search, fraud scoring, chatbot | [`com.bazaarhub.backend.ai`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/ai/README.md) |

---

## 3. Universal Module Internal Structure
Every single module strictly implements this identical Clean Architecture package layout:

```
<module>/
├── domain/
│   ├── model/
│   ├── valueobject/
│   ├── repository/
│   ├── service/
│   ├── event/
│   └── exception/
│
├── application/
│   ├── port/
│   │   ├── input/
│   │   └── output/
│   ├── usecase/
│   ├── command/
│   ├── query/
│   ├── dto/
│   └── mapper/
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── mapper/
│   ├── adapter/
│   └── config/
│
└── presentation/
    └── rest/
        ├── controller/
        ├── request/
        └── response/
```

## 4. Maintenance
Each module directory contains its own `README.md` defining domain entities, planned capabilities, and interface contracts.
