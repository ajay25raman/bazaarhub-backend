# BazaarHub Logistics – Internal Shipping & Shipment Tracking Module (Phase 12)

The **Internal Shipping and Shipment Tracking Module** (`com.bazaarhub.backend.shipping`) provides production-ready, enterprise-grade logistics fulfillment for BazaarHub using its internal delivery network. It supports multi-vendor seller-order-level shipments, package and dimensional specifications, a controlled state machine, public and authenticated tracking, distance/weight-based shipping charge computation, and seamless integration with Phase 11 Delivery Partner operations, Google Maps, Firebase Cloud Messaging, and Cloudinary.

---

## 1. Architectural Highlights

- **Clean Architecture & SOLID Separation**:
  - `domain`: Core domain models (`ShippingProviderType`, `PackageDetails`, `ShippingRateQuote`, `TrackingMilestone`, `PublicTrackingSummary`) and domain exceptions (`ShipmentAlreadyExistsException`, `InvalidShipmentOperationException`, `UnauthorizedShipmentAccessException`, `ShippingCalculationException`, `IneligibleOrderForShipmentException`).
  - `infrastructure`: Extensible `ShippingProvider` SPI with default `InternalShippingProvider` for BazaarHub's internal fleet, while providing clean extension points for future couriers (e.g. Shiprocket) without rewriting core business workflows.
  - `application`: DTO request/response contracts, `ShippingMapper`, `InternalShippingService`, `ShipmentTrackingService`, and `ShippingRateCalculator`.
  - `presentation.rest`: REST controllers segregated by user persona (`InternalShipmentController`, `SellerShipmentController`, `CustomerShipmentController`, `ShipmentTrackingController`, `AdminShipmentController`).

- **Database Safety & Integrity**:
  - Operates strictly on existing Hostinger MariaDB 11.8.9 tables (`shipments`, `delivery_assignments`, `delivery_tracking`, `seller_orders`, `orders`, `order_status_history`).
  - Strict compliance with `shipments.seller_order_id` `UNIQUE` constraint: guarantees exactly one shipment per vendor sub-order and prevents duplicate dispatches.
  - Fully compatible with `spring.jpa.hibernate.ddl-auto=validate`.

---

## 2. Shipment Lifecycle State Machine

```
              ┌──────────────┐
              │   CREATED    │ (status = PENDING)
              └──────┬───────┘
                     │ (Seller packages parcel & marks ready)
                     ▼
          ┌─────────────────────┐
          │  READY_FOR_PICKUP   │
          └──────────┬──────────┘
                     │ (Delivery partner accepts & confirms pickup)
                     ▼
              ┌──────────────┐
              │  PICKED_UP   │
              └──────┬───────┘
                     │ (Delivery partner en route)
                     ▼
              ┌──────────────┐
              │  IN_TRANSIT  │
              └──────┬───────┘
                     │ (Approaching destination - OTP dispatched)
                     ▼
          ┌─────────────────────┐
          │  OUT_FOR_DELIVERY   │
          └──────┬──────────┬───┘
 (OTP verified)  │          │ (Delivery attempt failed)
                 ▼          ▼
          ┌──────────┐  ┌────────┐
          │DELIVERED │  │ FAILED │
          └──────────┘  └───┬────┘
                            │ (Reassign / retry)
                            ▼
                     [READY_FOR_PICKUP]
```

Any pre-delivery status can transition to `CANCELLED` by Admin or by Seller (prior to pickup), which safely cascades to active delivery assignments.

---

## 3. Shipping Charge Calculation Engine

The `ShippingRateCalculator` calculates dynamic shipping charges on the backend using `BigDecimal`:
- **Base Fee**: ₹40.00
- **Distance Fee**: ₹5.00/km for distance exceeding the 5.0 km base radius (calculated via Google Maps Platform Directions API or Haversine fallback).
- **Weight Surcharge**: ₹10.00/kg for parcel weight exceeding the 1.0 kg threshold (aggregated from item variant weights).
- **Free Shipping Threshold**: Orders/sub-orders with subtotal $\ge$ ₹500.00 receive automated 100% shipping waiver (total = ₹0.00).
- **Maximum Cap**: ₹200.00 fee ceiling.

---

## 4. Privacy-First Tracking Protocols

1. **Public Tracking (`GET /api/v1/shipments/track/{trackingNumber}`)**:
   - Requires **no authentication** (`permitAll()`).
   - Masks all sensitive customer and delivery partner data.
   - Exposes only carrier name, status, origin city, destination city, destination state, timestamps, and milestone progress.

2. **Authenticated Tracking (`GET /api/v1/shipments/{shipmentId}/tracking-history`)**:
   - Enforces strict ownership checks (`ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_DELIVERY`, `ROLE_ADMIN`).
   - Surfaces delivery partner name, vehicle type, masked phone, live GPS breadcrumb coordinates (`delivery_tracking`), and parcel condition notes.

---

## 5. API Reference Summary

| Method | Endpoint | Description | Role / Authority |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/shipments` | Create shipment for seller sub-order | `SELLER`, `ADMIN` |
| `GET` | `/api/v1/shipments/{id}` | Get shipment details by ID | `CUSTOMER`, `SELLER`, `ADMIN` |
| `GET` | `/api/v1/shipments/order/{orderId}` | Get all vendor shipments for customer order | `CUSTOMER`, `ADMIN` |
| `POST` | `/api/v1/shipments/{id}/ready-for-pickup` | Seller marks parcel packaged and ready for pickup | `SELLER`, `ADMIN` |
| `POST` | `/api/v1/shipments/rate-quote` | Get real-time shipping rate quotation | `Public / All` |
| `GET` | `/api/v1/shipments/seller` | List authenticated seller's store shipments | `ROLE_SELLER`, `ADMIN` |
| `GET` | `/api/v1/shipments/seller/{id}` | Get seller shipment details | `ROLE_SELLER`, `ADMIN` |
| `GET` | `/api/v1/customer/shipments` | List customer's own package shipments | `ROLE_CUSTOMER`, `ADMIN` |
| `GET` | `/api/v1/customer/shipments/{id}` | Get customer package details | `ROLE_CUSTOMER`, `ADMIN` |
| `GET` | `/api/v1/shipments/track/{trackingNumber}` | Public tracking number lookup | `Public / All` |
| `GET` | `/api/v1/shipments/{id}/tracking-history` | Authenticated full tracking history with breadcrumbs | `CUSTOMER`, `SELLER`, `DELIVERY`, `ADMIN` |
| `GET` | `/api/v1/admin/shipments` | Admin paginated search & filter shipments | `ROLE_ADMIN` |
| `GET` | `/api/v1/admin/shipments/{id}` | Admin detailed shipment view | `ROLE_ADMIN` |
| `GET` | `/api/v1/admin/shipments/active` | Admin monitor active in-flight shipments | `ROLE_ADMIN` |
| `GET` | `/api/v1/admin/shipments/failed` | Admin inspect failed delivery shipments | `ROLE_ADMIN` |
| `POST` | `/api/v1/admin/shipments/{id}/cancel` | Admin cancel shipment and active assignment | `ROLE_ADMIN` |
