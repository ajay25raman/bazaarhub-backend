# Delivery & Logistics Operations Module (`com.bazaarhub.backend.delivery`)

## 1. Module Overview
The Delivery & Logistics module is an enterprise-grade autonomous vertical slice of the BazaarHub Modular Monolith responsible for:
- Delivery Partner (Rider) Onboarding, Vehicle Profiling, and Operational Availability Management
- Administrative KYC Verification (Approve/Reject) and Account Status Controls (Active/Suspended)
- Shipment Lifecycle Tracking and Automated Dispatch Management
- Delivery Task Assignment, Reassignment, Acceptance, and Rejection State Machine
- Store Parcel Pickup Confirmation with packaging condition logging
- Cryptographically Secure Delivery OTP Generation, Salted Hashing, and Verification
- Proof of Delivery (POD) and Evidence Document Archival via Cloudinary
- Real-Time GPS Tracking and Geolocation Logging (`delivery_tracking`)
- Google Maps Platform Route Modeling (Directions & Haversine fallback)
- Firebase Cloud Messaging (FCM) Push Notifications for Delivery Milestones
- Seamless Settlement Integration with Phase 10 Cash on Delivery (COD) Payment Flow

---

## 2. Architecture & Design Patterns

Strictly follows **Clean Architecture**, **SOLID principles**, and **Domain-Driven Design (DDD)**:

```
delivery/
├── domain/                                  # Pure Domain Rules (Zero Framework Dependencies)
│   ├── model/                               # Enums & Domain Value Objects
│   │   ├── VehicleType                      # BIKE, SCOOTER, BICYCLE, OTHER
│   │   ├── DeliveryBoyVerificationStatus    # PENDING, VERIFIED, REJECTED
│   │   ├── DeliveryBoyAvailabilityStatus    # AVAILABLE, BUSY, OFFLINE, SUSPENDED
│   │   ├── ShipmentStatus                   # READY_FOR_PICKUP, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, FAILED
│   │   ├── DeliveryAssignmentStatus         # ASSIGNED, ACCEPTED, REJECTED, PICKED_UP, COMPLETED, CANCELLED
│   │   └── DeliveryFailureReason            # CUSTOMER_UNAVAILABLE, INCORRECT_ADDRESS, etc.
│   └── exception/                           # Domain Exceptions
│       ├── DeliveryBoyNotFoundException
│       ├── DeliveryAssignmentNotFoundException
│       ├── ShipmentNotFoundException
│       ├── DeliveryBoyAlreadyExistsException
│       ├── UnauthorizedDeliveryAccessException
│       ├── InvalidDeliveryOperationException
│       └── InvalidDeliveryOtpException
│
├── application/                             # Application Orchestration & Use Cases
│   ├── dto/
│   │   ├── request/                         # Strongly validated request contracts
│   │   └── response/                        # Standardized response models
│   ├── mapper/                              # Entity-to-DTO Mappers (DeliveryMapper)
│   └── service/                             # Business Services & Cryptographic Handlers
│       ├── DeliveryBoyService & Impl
│       ├── ShipmentService & Impl
│       ├── DeliveryAssignmentService & Impl
│       ├── DeliveryTrackingService & Impl
│       └── DeliveryOtpService               # SecureRandom 6-digit OTP + Salted SHA-256
│
├── infrastructure/                          # Adapters & Persistence Layer
│   ├── adapter/                             # Third-party service integrations
│   │   ├── CloudinaryService                # Image & proof upload adapter
│   │   ├── GoogleMapsService                # Directions API & distance estimator
│   │   └── FcmPushNotificationService       # FCM Push Notification dispatcher
│   └── persistence/
│       ├── entity/                          # MariaDB JPA Entities (ddl-auto=validate compatible)
│       │   ├── DeliveryBoyEntity            # maps delivery_boys
│       │   ├── ShipmentEntity               # maps shipments
│       │   ├── DeliveryAssignmentEntity     # maps delivery_assignments
│       │   └── DeliveryTrackingEntity       # maps delivery_tracking
│       └── repository/                      # Spring Data JPA Repositories
│
└── presentation/rest/                       # Thin REST Controllers
    ├── DeliveryBoyController                # /api/v1/delivery/**
    ├── AdminDeliveryController              # /api/v1/admin/delivery/**
    └── CustomerDeliveryTrackingController   # /api/v1/delivery/tracking/**
```

---

## 3. Database Schema Mapping

Entities strictly map to MariaDB 11.8.9 tables without any DDL modifications:

| Database Table | Entity Class | Primary Responsibility |
|----------------|--------------|------------------------|
| `delivery_boys` | `DeliveryBoyEntity` | Delivery partner profile, vehicle type, KYC status, and availability |
| `shipments` | `ShipmentEntity` | Shipment dispatch record, carrier details, transit status, and delivery notes |
| `delivery_assignments` | `DeliveryAssignmentEntity` | Assignment task lifecycle between delivery partner and shipment |
| `delivery_tracking` | `DeliveryTrackingEntity` | Real-time GPS coordinate trail recorded during active delivery |

---

## 4. API Endpoints Reference

### Delivery Partner APIs (`/api/v1/delivery`)
- `POST /api/v1/delivery/onboard`: Register as delivery partner (assigns `ROLE_DELIVERY`)
- `GET /api/v1/delivery/profile`: View delivery partner profile and verification status
- `PUT /api/v1/delivery/profile`: Update vehicle details
- `PATCH /api/v1/delivery/availability`: Toggle operational status (`AVAILABLE` / `OFFLINE`)
- `GET /api/v1/delivery/assignments`: List assigned deliveries (filterable by status)
- `GET /api/v1/delivery/assignments/{id}`: Detailed assignment info (pickup, delivery addresses, COD)
- `POST /api/v1/delivery/assignments/{id}/accept`: Accept assigned delivery task
- `POST /api/v1/delivery/assignments/{id}/reject`: Reject delivery task with mandatory reason
- `POST /api/v1/delivery/assignments/{id}/pickup`: Confirm parcel collection from seller store
- `POST /api/v1/delivery/assignments/{id}/out-for-delivery`: Mark out for delivery (triggers OTP generation & SMS/FCM alert)
- `POST /api/v1/delivery/assignments/{id}/complete`: Complete delivery (verifies OTP, collects COD cash if applicable)
- `POST /api/v1/delivery/assignments/{id}/fail`: Mark delivery attempt failed with business reason
- `POST /api/v1/delivery/tracking/location`: Post live GPS coordinates
- `GET /api/v1/delivery/route-estimate/{shipmentId}`: Get route distance and ETA via Google Maps
- `POST /api/v1/delivery/proof/upload`: Upload proof photo to Cloudinary

### Admin Delivery APIs (`/api/v1/admin/delivery`)
- `GET /api/v1/admin/delivery/boys`: List delivery partners (paginated, filterable)
- `GET /api/v1/admin/delivery/boys/{id}`: View partner profile and history
- `PATCH /api/v1/admin/delivery/boys/{id}/verify`: Approve/Reject partner KYC documentation
- `PATCH /api/v1/admin/delivery/boys/{id}/status`: Suspend or activate partner account
- `POST /api/v1/admin/delivery/shipments`: Create shipment for seller sub-order
- `GET /api/v1/admin/delivery/shipments`: List shipments (filterable)
- `GET /api/v1/admin/delivery/shipments/{id}`: Get shipment details
- `POST /api/v1/admin/delivery/assignments`: Assign delivery partner to shipment
- `POST /api/v1/admin/delivery/assignments/{id}/reassign`: Reassign shipment to another partner
- `GET /api/v1/admin/delivery/assignments`: View all assignments across platform
- `GET /api/v1/admin/delivery/tracking/{shipmentId}`: View GPS breadcrumb tracking trail

### Customer Delivery Tracking APIs (`/api/v1/delivery/tracking`)
- `GET /api/v1/delivery/tracking/{shipmentId}`: View latest GPS location of delivery partner
- `GET /api/v1/delivery/tracking/{shipmentId}/estimate`: View remaining transit distance and ETA
- `GET /api/v1/delivery/tracking/shipments/my`: View active shipments for authenticated customer

---

## 5. Security & Verification Rules

1. **OTP Security**:
   - 6-digit numeric OTP generated via `SecureRandom`.
   - Stored in memory as a salted SHA-256 hash (never in plaintext).
   - Never logged in application logs.
   - Never returned in Delivery Boy API responses.
   - Max 3 failed verification attempts before locking.
   - Automatic 2-hour TTL expiration.
   - Invalidated immediately upon successful delivery or failure.
2. **COD Cash Collection**:
   - Reconciles with Phase 10 `PaymentService`.
   - Records `PaymentTransactionEntity` for collected cash.
   - Transitions `PaymentEntity` to `CAPTURED` and `OrderEntity` to `PAID`.
3. **Role-Based Access Control**:
   - `/api/v1/delivery/onboard`: Authenticated users.
   - `/api/v1/delivery/**`: `ROLE_DELIVERY` or `ROLE_ADMIN`.
   - `/api/v1/admin/delivery/**`: `ROLE_ADMIN`.
   - Ownership checks strictly enforced (riders cannot view or update other riders' assignments).
