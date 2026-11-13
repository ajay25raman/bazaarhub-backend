# Identity & Access Management Module (`com.bazaarhub.backend.identity`)

## 1. Module Overview
The Identity module manages user authentication, authorization, multi-factor authentication (MFA), role-based access control (RBAC), and session security across Customer, Seller, Delivery Partner, and Admin personas.

This module is designed as an autonomous vertical slice of the BazaarHub Modular Monolith, strictly adhering to Clean Architecture and Domain-Driven Design (DDD) principles.

---

## 2. Business Responsibilities
- User identity lifecycle management (registration, activation, deactivation)
- Secure authentication via credentials, OTP, and OAuth2/OIDC social logins
- Role-based access control (RBAC) and granular permission enforcement
- JWT token issuance, signing, validation, and refresh token rotation
- Multi-Factor Authentication (MFA) via TOTP and SMS
- Audit logging of all authentication and security-critical events

---

## 3. Implementation Status & Roadmap

### Implemented Functionality (Phase 1 Foundation)
- [x] Clean Architecture folder hierarchy (`domain`, `application`, `infrastructure`, `presentation`).
- [x] Module boundary isolation within `com.bazaarhub.backend.identity`.
- [x] Layer package definitions and architectural contracts.
- [x] Unit and slice test package scaffolding under `src/test/java/com/bazaarhub/backend/identity`.

### Planned Functionality (Subsequent Phases)
- [ ] Stateless JWT access and refresh token authentication
- [ ] Password hashing with Argon2id / BCrypt
- [ ] Role and permission evaluation engine
- [ ] Account lockout on repeated failed attempts
- [ ] OAuth2 / Google & Apple social sign-in integrations

---

## 4. Clean Architecture Layer Responsibilities

```
identity/
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
- **External Module Access**: Depends solely on shared kernel (domain exceptions, presentation responses). Completely decoupled from business domains.

### Cross-Module Communication Rules
1. **No Shared Database Tables**: This module owns its data model exclusively. No other module may query this module's tables directly.
2. **Public Contracts Only**: Other modules must communicate strictly via public Application Input Ports or asynchronous Domain Events.
3. **No Circular Dependencies**: Circular dependencies between `identity` and other modules are strictly prohibited.
4. Other modules consume Identity via security context and token claims (Principal ID, Roles). Identity never queries business module databases.

---

## 6. Future Implementation Roadmap
1. Define domain entities, value objects, and domain events.
2. Implement application use cases and validation rules.
3. Add repository output ports and mock test doubles.
4. Expose REST endpoints with OpenAPI annotations and Spring Validation.
5. In Phase 2: Add database persistence entities, Flyway migrations, and repository adapters.
