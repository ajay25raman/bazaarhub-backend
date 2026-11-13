# Promotion & Discount Module (`com.bazaarhub.backend.promotion`)

## 1. Module Overview
The Promotion module manages promotional coupons, percentage/fixed discounts, flash sale events, cart-level rules, bundle deals, and user usage limits.

This module is designed as an autonomous vertical slice of the BazaarHub Modular Monolith, strictly adhering to Clean Architecture and Domain-Driven Design (DDD) principles.

---

## 2. Business Responsibilities
- Coupon code lifecycle (creation, scheduling, expiration, cancellation)
- Promotion rule engine (minimum cart value, category restriction, user usage limits)
- Flash sale event scheduling and item discount overrides
- Multi-vendor vs platform-funded promotional subsidies
- Coupon redemption tracking and prevention of double-use

---

## 3. Implementation Status & Roadmap

### Implemented Functionality (Phase 1 Foundation)
- [x] Clean Architecture folder hierarchy (`domain`, `application`, `infrastructure`, `presentation`).
- [x] Module boundary isolation within `com.bazaarhub.backend.promotion`.
- [x] Layer package definitions and architectural contracts.
- [x] Unit and slice test package scaffolding under `src/test/java/com/bazaarhub/backend/promotion`.

### Planned Functionality (Subsequent Phases)
- [ ] Discount evaluation engine applying best-value discounts to carts
- [ ] Flash sale countdown scheduler and concurrency control
- [ ] Atomic coupon usage counter with concurrency reservation
- [ ] Platform vs Seller discount subsidy accounting

---

## 4. Clean Architecture Layer Responsibilities

```
promotion/
├── domain/                  # Core Business Logic (Framework-independent)
│   ├── model/               # Domain Entities and Aggregates
│   ├── valueobject/         # Immutable Value Objects
│   ├── repository/          # Repository Abstractions (SPI)
│   ├── service/             # Domain Services (multi-entity logic)
│   ├── event/               # Domain Events published upon state transitions
│   └── exception/           # Business and Domain-specific Exceptions
│
├── application/             # Application Orchestration & Use Cases
│   ├── port/
│   │   ├── input/           # Input Ports (Use Case Interfaces)
│   │   └── output/          # Output Ports (SPI for persistence, external services)
│   ├── usecase/             # Use Case Implementations
│   ├── command/             # State-mutating Command objects
│   ├── query/               # Read-only Query objects
│   ├── dto/                 # Application Data Transfer Objects
│   └── mapper/              # Domain-to-DTO and Command-to-Domain mappers
│
├── infrastructure/          # Technical Details & External Adapters
│   ├── persistence/
│   │   ├── entity/          # Database/ORM Entities (e.g., JPA/MariaDB in future phases)
│   │   ├── repository/      # Spring Data Repositories & Output Port Implementations
│   │   └── mapper/          # Entity-to-Domain Model Mappers
│   ├── adapter/             # External service adapters (e.g., messaging, cloud SDKs)
│   └── config/              # Module-specific Spring configuration beans
│
└── presentation/            # External Delivery Mechanism (HTTP/REST)
    └── rest/
        ├── controller/      # REST API Controllers (Thin adapters)
        ├── request/         # Incoming HTTP Request DTOs with validation rules
        └── response/        # Outgoing HTTP Response DTOs
```

### Layer Rules
1. **Domain Layer**: Contains enterprise business rules. Has ZERO dependencies on Spring Framework, database drivers, or HTTP frameworks.
2. **Application Layer**: Coordinates use cases, transaction boundaries, and security rules. Depends solely on domain abstractions.
3. **Infrastructure Layer**: Implements output ports defined by application/domain. Contains database entities, Spring Data repositories, and third-party adapters.
4. **Presentation Layer**: Thin REST controllers validating incoming HTTP payloads and delegating immediately to application input ports.

---

## 5. Module Dependencies & Communication Rules

### Dependencies
- **Shared Kernel**: `com.bazaarhub.backend.shared` (common domain exceptions, standardized API response wrapper, base value objects).
- **External Module Access**: Interacts with Cart and Order. Depends on Shared kernel.

### Cross-Module Communication Rules
1. **No Shared Database Tables**: This module owns its data model exclusively. No other module may query this module's tables directly.
2. **Public Contracts Only**: Other modules must communicate strictly via public Application Input Ports or asynchronous Domain Events.
3. **No Circular Dependencies**: Circular dependencies between `promotion` and other modules are strictly prohibited.
4. Cart and Order query PromotionValidationPort; Order confirms coupon redemption via event.

---

## 6. Future Implementation Roadmap
1. Define domain entities, value objects, and domain events.
2. Implement application use cases and validation rules.
3. Add repository output ports and mock test doubles.
4. Expose REST endpoints with OpenAPI annotations and Spring Validation.
5. In Phase 2: Add database persistence entities, Flyway migrations, and repository adapters.
