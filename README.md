# BazaarHub Backend

> **Enterprise AI-Powered Multi-Vendor E-Commerce Marketplace**  
> Architected as a Modular Monolith utilizing Clean Architecture and Domain-Driven Design (DDD) principles on Java 21 and Spring Boot 4.1.2.

---

## 1. Project Introduction
**BazaarHub** is an enterprise-grade, high-scale multi-vendor e-commerce marketplace platform engineered to bridge buyers, vendors, fulfillment partners, and platform administrators. Built with a focus on high concurrency, maintainability, domain isolation, and rapid feature iteration, BazaarHub serves as the centralized backend powering four distinct Flutter-based client applications.

---

## 2. Business Objectives
- **Multi-Vendor Enablement**: Seamless vendor onboarding, KYC compliance, storefront customization, catalog authoring, and commission-based payouts.
- **Transactional Reliability**: Atomically coordinated checkout sagas, distributed-ready inventory reservation locks, and multi-gateway payment processing.
- **AI-Enhanced Commerce**: Intelligent product recommendations, semantic vector search, dynamic pricing optimization, and fraud anomaly detection.
- **Scalable Logistics**: Automated proximity-based delivery routing, real-time geotracking, and proof-of-delivery (POD) verification.
- **Enterprise Governance**: Granular role-based access control (RBAC), multi-party dispute arbitration, and immutable financial ledger accounting.

---

## 3. Platform Overview
The BazaarHub platform ecosystem connects four specialized applications to a centralized backend:
1. **Customer App** (*Flutter Android / iOS*): Intuitive catalog browsing, search, cart, checkout, order tracking, and reviews.
2. **Seller App** (*Flutter Android / iOS*): Real-time store management, inventory updates, sub-order fulfillment, and payout tracking.
3. **Delivery Boy App** (*Flutter Android / iOS*): Delivery task assignment, turn-by-turn navigation, status updates, and OTP proof-of-delivery.
4. **Admin Panel** (*Flutter Web*): Comprehensive governance, vendor approvals, catalog taxonomy moderation, platform telemetry, and dispute resolution.
5. **Centralized Backend** (*Java Spring Boot*): The Modular Monolith powering all platform logic, security, and persistence.

---

## 4. Technology Stack
- **Language / Runtime**: Java 21 (LTS)
- **Framework**: Spring Boot 4.1.2
- **Build & Dependency Management**: Apache Maven (via Maven Wrapper `./mvnw`)
- **Web Layer**: Spring WebMVC (RESTful JSON APIs)
- **Security**: Spring Security (Stateless JWT token architecture, RBAC)
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Code Generation**: Project Lombok
- **Observability**: Spring Boot Actuator (`health`, `info`, `metrics`)
- **Testing**: JUnit 5, Mockito
- **Containerization**: Docker (Multi-stage build, non-root execution)
- **Orchestration**: Docker Compose (Staging & Production isolation)
- **Image Registry**: GitHub Container Registry (GHCR: `ghcr.io/bazaarhub/bazaarhub-backend`)
- **CI/CD Platform**: GitHub Actions (Verify, Build & Push, Staging & Production Deploy)
- **Hosting Target**: Hostinger Linux VPS (Ubuntu 22.04 / 24.04 LTS)
- **Database Status**: Phase 1 Foundation operates database-free (in-memory execution).

---

## 5. Architecture Overview
BazaarHub utilizes a **Modular Monolith** architecture combined with **Clean Architecture** (Ports and Adapters / Hexagonal) inside every business module.

```
+-----------------------------------------------------------------------------------------+
|                                    CLIENT APPLICATIONS                                  |
|   Customer App (Flutter) | Seller App (Flutter) | Delivery App (Flutter) | Admin (Web)  |
+-----------------------------------------------------------------------------------------+
                                             |
                                        HTTP / REST
                                             v
+-----------------------------------------------------------------------------------------+
|                                  BAZAARHUB BACKEND                                      |
|                                (Modular Monolith)                                       |
|                                                                                         |
|   +-------------------+  +-------------------+  +-------------------+  +------------+   |
|   |     identity      |  |     customer      |  |      seller       |  |   store    |   |
|   +-------------------+  +-------------------+  +-------------------+  +------------+   |
|   |      catalog      |  |     inventory     |  |       cart        |  |  wishlist  |   |
|   +-------------------+  +-------------------+  +-------------------+  +------------+   |
|   |       order       |  |      payment      |  |      refund       |  |  delivery  |   |
|   +-------------------+  +-------------------+  +-------------------+  +------------+   |
|   |    commission     |  |      finance      |  |     promotion     |  |   review   |   |
|   +-------------------+  +-------------------+  +-------------------+  +------------+   |
|   |   notification    |  |      support      |  |       admin       |  |     ai     |   |
|   +-------------------+  +-------------------+  +-------------------+  +------------+   |
|                                                                                         |
|   +---------------------------------------+  +--------------------------------------+   |
|   |          shared (Kernel)              |  |         bootstrap (Root)             |   |
|   +---------------------------------------+  +--------------------------------------+   |
+-----------------------------------------------------------------------------------------+
```

---

## 6. Clean Architecture Explanation
Every business module is partitioned into concentric layers adhering to the **Dependency Inversion Principle**:

```
Presentation (REST) -> Application (Use Cases) -> Domain (Pure Business Core)
Infrastructure (Persistence / External) -> Application / Domain Abstractions
```

### Layer Responsibilities
- **Domain Layer (`domain/`)**: Pure business logic, aggregates, entities, value objects, domain events, domain exceptions, and repository interfaces (SPI). Free of Spring Framework dependencies.
- **Application Layer (`application/`)**: Application use cases, input ports, output ports, commands, queries, DTOs, and mappers. Coordinates domain objects to satisfy user interactions.
- **Infrastructure Layer (`infrastructure/`)**: Technical adapters, persistence implementations, third-party service gateways, and Spring `@Configuration` beans.
- **Presentation Layer (`presentation/rest/`)**: Thin REST controllers, request/response models, and validation logic. Calls application input ports.

---

## 7. Modular Monolith Explanation
Rather than adopting distributed microservices prematurely, BazaarHub deploys as a single, highly-performant JVM artifact while maintaining strict module encapsulation:
- **No Shared Database Tables**: Each module owns its data model exclusively.
- **Zero Cross-Module SQL Joins**: Foreign keys across modules are maintained as soft references (UUIDs/IDs).
- **Public Communication Ports**: Modules interact strictly via Application Input Ports or asynchronous Domain Events.
- **Effortless Microservice Extraction**: Any module can be extracted into an independent microservice in the future if required.

---

## 8. Complete Business Module List (20 Modules)

| # | Module | Scope & Domain Responsibility | Package Path |
| :---: | :--- | :--- | :--- |
| 1 | `identity` | Authentication, Authorization, RBAC, JWT tokens, MFA, Sessions | [`com.bazaarhub.backend.identity`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/identity/README.md) |
| 2 | `customer` | Buyer profile, address book, preferences, GDPR compliance | [`com.bazaarhub.backend.customer`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/customer/README.md) |
| 3 | `seller` | Vendor onboarding, KYC verification, legal details, badges | [`com.bazaarhub.backend.seller`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/seller/README.md) |
| 4 | `store` | Storefront management, operating hours, geolocation radius | [`com.bazaarhub.backend.store`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/store/README.md) |
| 5 | `catalog` | Products, taxonomies, categories, brands, variants, SKUs | [`com.bazaarhub.backend.catalog`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/catalog/README.md) |
| 6 | `inventory` | Stock tracking, warehouse allocation, checkout reservation | [`com.bazaarhub.backend.inventory`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/inventory/README.md) |
| 7 | `cart` | Active shopping carts (guest/authenticated), item validation | [`com.bazaarhub.backend.cart`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/cart/README.md) |
| 8 | `wishlist` | Saved items, price drop tracking, wishlist sharing | [`com.bazaarhub.backend.wishlist`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/wishlist/README.md) |
| 9 | `order` | Order placement, state machine, multi-vendor sub-orders | [`com.bazaarhub.backend.order`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/order/README.md) |
| 10 | `payment` | Payment intents, gateway adapters (Stripe, etc.), webhooks | [`com.bazaarhub.backend.payment`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/payment/README.md) |
| 11 | `refund` | Refund requests, return authorization, payment reversal | [`com.bazaarhub.backend.refund`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/refund/README.md) |
| 12 | `delivery` | Delivery partner assignment, dispatch, tracking, POD verification | [`com.bazaarhub.backend.delivery`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/delivery/README.md) |
| 13 | `commission` | Platform fee calculation, seller tier discount, tax on fees | [`com.bazaarhub.backend.commission`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/commission/README.md) |
| 14 | `finance` | Vendor payouts, double-entry ledger, escrow, tax withholding | [`com.bazaarhub.backend.finance`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/finance/README.md) |
| 15 | `promotion` | Coupons, flash sales, discount engine, usage limits | [`com.bazaarhub.backend.promotion`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/promotion/README.md) |
| 16 | `review` | Ratings, product/store reviews, moderation, verified buyer | [`com.bazaarhub.backend.review`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/review/README.md) |
| 17 | `notification` | Push notifications (FCM), SMS, email, in-app inbox | [`com.bazaarhub.backend.notification`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/notification/README.md) |
| 18 | `support` | Helpdesk ticketing, buyer-seller disputes, resolution | [`com.bazaarhub.backend.support`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/README.md) |
| 19 | `admin` | Platform governance, analytics, moderation, audit review | [`com.bazaarhub.backend.admin`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/admin/README.md) |
| 20 | `ai` | Recommendations, semantic search, fraud scoring, chatbot | [`com.bazaarhub.backend.ai`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/ai/README.md) |

---

## 9. Complete Project Directory Tree

```
bazaarhub-backend/
├── .github/
│   └── workflows/
│       ├── backend-ci.yml             # PR and main branch CI validation
│       ├── build-and-push.yml         # Container build and GHCR push
│       ├── deploy-staging.yml         # Automated Hostinger Staging deploy
│       └── deploy-production.yml      # Release tag Hostinger Production deploy
├── deployment/
│   ├── compose/
│   │   ├── docker-compose.staging.yml     # Staging Compose configuration (Port 8081)
│   │   └── docker-compose.production.yml  # Production Compose configuration (Port 8080)
│   ├── docker/
│   │   ├── Dockerfile                 # Multi-stage Java 21 Temurin build
│   │   └── .dockerignore              # Build context exclusions
│   └── scripts/
│       ├── health-check.sh            # Configurable smoke & health test probe
│       └── rollback.sh                # Instant zero-downtime rollback script
├── docs/
│   ├── README.md
│   ├── api/
│   │   └── README.md
│   ├── architecture/
│   │   ├── README.md
│   │   ├── decisions/
│   │   │   ├── ADR-001-modular-monolith.md
│   │   │   ├── ADR-002-clean-architecture.md
│   │   │   ├── ADR-003-package-by-feature.md
│   │   │   ├── ADR-004-domain-independence.md
│   │   │   ├── ADR-005-module-boundaries.md
│   │   │   └── README.md
│   │   ├── diagrams/
│   │   │   └── README.md
│   │   └── overview/
│   │       └── README.md
│   ├── database/
│   │   └── README.md
│   ├── deployment/
│   │   ├── README.md
│   │   ├── ci-cd-pipeline.md          # CI/CD topography and least-privilege matrix
│   │   ├── staging-deployment.md      # Staging deployment guide
│   │   ├── production-deployment.md   # Production release guide
│   │   ├── rollback-strategy.md       # State tracking and rollback mechanics
│   │   └── server-setup.md            # Hostinger VPS hardening & reverse proxy setup
│   ├── interview-preparation/
│   │   ├── README.md
│   │   └── phase-1-architecture-explanation.md
│   ├── modules/
│   │   └── README.md
│   ├── security/
│   │   └── README.md
│   └── testing/
│       └── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── HELP.md
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── bazaarhub/
│   │   │           └── backend/
│   │   │               ├── BazaarhubBackendApplication.java
│   │   │               ├── bootstrap/
│   │   │               │   ├── package-info.java
│   │   │               │   ├── config/
│   │   │               │   │   └── package-info.java
│   │   │               │   └── initialization/
│   │   │               │       └── package-info.java
│   │   │               ├── shared/
│   │   │               │   ├── package-info.java
│   │   │               │   ├── application/
│   │   │               │   │   └── port/
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exception/
│   │   │               │   │   │   ├── BusinessRuleViolationException.java
│   │   │               │   │   │   ├── DomainException.java
│   │   │               │   │   │   └── ResourceNotFoundException.java
│   │   │               │   │   └── valueobject/
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── config/
│   │   │               │   │   └── logging/
│   │   │               │   └── presentation/
│   │   │               │       └── rest/
│   │   │               │           ├── exception/
│   │   │               │           │   └── GlobalRestExceptionHandler.java
│   │   │               │           ├── health/
│   │   │               │           │   └── HealthController.java
│   │   │               │           └── response/
│   │   │               │               └── ApiResponse.java
│   │   │               └── [identity | customer | seller | store | catalog | ... (20 modules)]/
│   │   │                   ├── README.md
│   │   │                   ├── package-info.java
│   │   │                   ├── domain/
│   │   │                   ├── application/
│   │   │                   ├── infrastructure/
│   │   │                   └── presentation/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-test.properties
│   │       └── db/
│   │           └── migration/
│   └── test/
│       └── java/
│           └── com/
│               └── bazaarhub/
│                   └── backend/
│                       ├── BazaarhubBackendApplicationTests.java
│                       ├── shared/
│                       └── [20 module test packages]/
```

---

## 10. Prerequisites
- **Java Development Kit (JDK)**: Java 21 (Eclipse Temurin, Amazon Corretto, or Oracle JDK 21).
- **Environment**: macOS, Linux, or Windows (WSL recommended).
- **Build Tool**: Maven 3.9+ (bundled via `./mvnw`).
- **Container Engine**: Docker 24.0+ and Docker Compose V2 (for containerized execution).

---

## 11. Local Development Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/bazaarhub/bazaarhub-backend.git
   cd bazaarhub-backend
   ```
2. Verify Java version:
   ```bash
   java -version  # Must output Java 21.x
   ```
3. Compile the project:
   ```bash
   ./mvnw clean compile
   ```

---

## 12. How to Run the Backend

### Option A: Local Maven Run
Launch the application locally using the Spring Boot Maven plugin:
```bash
./mvnw spring-boot:run
```
The application boots on `http://localhost:8080`.

Verify server health:
```bash
curl http://localhost:8080/api/v1/health
```

### Option B: Local Docker Build & Run
Build the container image using the multi-stage Dockerfile:
```bash
docker build -t bazaarhub-backend:local -f deployment/docker/Dockerfile .
docker run -p 8080:8080 bazaarhub-backend:local
```

---

## 13. How to Run Tests
Execute the automated test suite via the Maven wrapper:
```bash
# Run all unit and integration tests
./mvnw clean verify

# Run a specific test class
./mvnw test -Dtest=BazaarhubBackendApplicationTests
```

---

## 14. Environment Configuration
Configuration files are organized under `src/main/resources/`:
- `application.properties`: Shared base settings (application name, default port `8080`, actuator endpoint exposure).
- `application-dev.properties`: Development profile with verbose debug logging and detailed metrics.
- `application-test.properties`: Test profile with dynamic ephemeral port allocation (`server.port=0`).

To activate a specific profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 15. Security Overview
- **Framework**: Spring Security 6.x.
- **Model**: Stateless token-based authentication (JWT) with Role-Based Access Control (`ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_DELIVERY`, `ROLE_ADMIN`).
- **Validation**: Strict server-side Jakarta Bean Validation on all incoming presentation models.
- **Container Security**: Non-root user `bazaarhub` (UID `10001`), read-only root filesystems where appropriate, and strict network isolation via internal loopback bindings.
- **Phase 1 State**: Standard Spring Security defaults active on the classpath.

---

## 16. Database Integration Status
- **Current Status**: **Database Not Configured**.
- In strict adherence to Phase 1 constraints, no JPA, Hibernate, MariaDB, Flyway, or Redis dependencies exist.
- The application runs, compiles, and passes tests entirely in-memory.
- `src/main/resources/db/migration/` is preserved for Phase 2 schema migrations.

---

## 17. API Documentation Strategy
- Unified response envelope (`ApiResponse<T>`) across all REST endpoints.
- Global exception mapping via `GlobalRestExceptionHandler`.
- Phase 2 will introduce SpringDoc OpenAPI 3 (`swagger-ui`) for interactive API documentation at `/swagger-ui.html`.

---

## 18. CI/CD & Deployment Architecture
BazaarHub implements an enterprise GitHub Actions CI/CD pipeline deploying to Hostinger VPS:
- **Continuous Integration**: [`.github/workflows/backend-ci.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/backend-ci.yml) executes on every PR and commit to `main`, validating Java 21 compilation and test verification.
- **Container Packaging**: [`.github/workflows/build-and-push.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/build-and-push.yml) builds and pushes immutable images to GitHub Container Registry (GHCR).
- **Staging Deployment**: [`.github/workflows/deploy-staging.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/deploy-staging.yml) deploys verified `main` builds to `/opt/bazaarhub/staging/` on port `8081`.
- **Production Deployment**: [`.github/workflows/deploy-production.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/deploy-production.yml) deploys immutable `v*` release tags to `/opt/bazaarhub/production/` on port `8080`.
- **Reverse Proxy**: Nginx or Caddy provides TLS termination and routes external HTTPS traffic to local loopback application ports.
- **Detailed Guides**: See [docs/deployment/](file:///Users/ajaykumar/bazaarhub-backend/docs/deployment/README.md).

---

## 19. Development Roadmap
- [x] **Phase 1: Architecture Foundation**: Clean Architecture folder hierarchy, 20 business modules, shared foundation, bootstrap root, documentation, ADRs, and database-free verification.
- [x] **Phase 1.5: Enterprise CI/CD & Deployment**: Multi-stage Dockerfile, Docker Compose for Staging/Production, GHCR integration, rollback automation, health probe scripts, and Hostinger VPS deployment workflows.
- [ ] **Phase 2: Persistence & Migrations**: MariaDB / PostgreSQL datasource configuration, Flyway migrations, JPA entity mappings, and Redis caching.
- [ ] **Phase 3: Identity & Access Management**: User registration, Argon2id password hashing, JWT token issuance, refresh rotation, and RBAC filters.
- [ ] **Phase 4: Core Commerce Engine**: Catalog products, inventory reservation locks, active cart management, and multi-vendor checkout saga.
- [ ] **Phase 5: Logistics & Payment**: Stripe/Razorpay gateway integrations, delivery geotracking, commission settlement, and notification pipelines.

---

## 20. Contribution Guidelines
1. Adhere strictly to the Dependency Inversion Principle: Domain layer must never import Spring Framework or Infrastructure classes.
2. Maintain module boundaries: Modules must never query other modules' persistence entities or internal repositories.
3. Every new business requirement must be accompanied by unit tests in the corresponding test package.
4. Any architectural change requires submitting a new Architecture Decision Record (ADR) under `docs/architecture/decisions/`.
