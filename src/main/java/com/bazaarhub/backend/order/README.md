# Order Management Module (`com.bazaarhub.backend.order`)

## 1. Module Overview
The Order module is the central transaction coordinator managing order creation, state machine transitions, multi-vendor sub-order splitting, and immutable order history.

This module is designed as an autonomous vertical slice of the BazaarHub Modular Monolith, strictly adhering to Clean Architecture and Domain-Driven Design (DDD) principles.

---

## 2. Business Responsibilities
- Order placement orchestration with validation and checkout sagas
- Order state machine management (CREATED, PENDING_PAYMENT, CONFIRMED, PROCESSING, DISPATCHED, DELIVERED, CANCELLED, RETURNED)
- Multi-vendor sub-order decomposition per store
- Immutable snapshotting of product, pricing, tax, and address data
- Order cancellation policies and time-window enforcement

---

## 3. Implementation Status & Roadmap

### Implemented Functionality (Phase 1 Foundation)
- [x] Clean Architecture folder hierarchy (`domain`, `application`, `infrastructure`, `presentation`).
- [x] Module boundary isolation within `com.bazaarhub.backend.order`.
- [x] Layer package definitions and architectural contracts.
- [x] Unit and slice test package scaffolding under `src/test/java/com/bazaarhub/backend/order`.

### Planned Functionality (Subsequent Phases)
- [ ] Checkout orchestrator coordinating Cart, Inventory, Promotion, and Payment
- [ ] Strict order state transition guards preventing illegal state jumps
- [ ] Sub-order generation allowing vendors to fulfill their items independently
- [ ] Order lifecycle timeline audit trail

---

## 4. Clean Architecture Layer Responsibilities

```
order/
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
- **External Module Access**: Interacts with Customer, Store, Catalog, Inventory, Payment, Delivery, Commission. Depends on Shared kernel.

### Cross-Module Communication Rules
1. **No Shared Database Tables**: This module owns its data model exclusively. No other module may query this module's tables directly.
2. **Public Contracts Only**: Other modules must communicate strictly via public Application Input Ports or asynchronous Domain Events.
3. **No Circular Dependencies**: Circular dependencies between `order` and other modules are strictly prohibited.
4. Coordinates checkout via application ports; emits OrderCreatedEvent, OrderConfirmedEvent, OrderCancelledEvent.

---

## 6. Future Implementation Roadmap
1. Define domain entities, value objects, and domain events.
2. Implement application use cases and validation rules.
3. Add repository output ports and mock test doubles.
4. Expose REST endpoints with OpenAPI annotations and Spring Validation.
5. In Phase 2: Add database persistence entities, Flyway migrations, and repository adapters.
