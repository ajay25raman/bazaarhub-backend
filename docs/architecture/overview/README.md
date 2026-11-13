# Architecture Overview

## 1. Purpose
This document provides an executive architectural overview of the BazaarHub backend, explaining how the platform satisfies high-concurrency enterprise marketplace demands while maintaining maintainability and developer ergonomics.

## 2. High-Level Blueprint
BazaarHub powers a multi-vendor marketplace connecting four client applications:
1. **Customer Mobile Application** (Flutter Android/iOS)
2. **Seller Mobile Application** (Flutter Android/iOS)
3. **Delivery Boy Mobile Application** (Flutter Android/iOS)
4. **Admin Web Dashboard** (Flutter Web)

All client platforms interface with a single centralized Java Spring Boot backend via standardized RESTful APIs and real-time event mechanisms.

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

## 3. Technology Stack & Constraints
- **Runtime**: Java 21 (LTS) leveraging modern language features (records, pattern matching, virtual threads).
- **Framework**: Spring Boot 4.1.2.
- **Web Layer**: Spring WebMVC with thin REST controllers.
- **Security**: Spring Security for authentication filters and authorization.
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`).
- **Observability**: Spring Boot Actuator (`/actuator/health`, `/actuator/info`).
- **Testing**: JUnit 5, Mockito.
- **Phase 1 Constraint**: Zero database dependencies. The application compiles, starts, and executes tests entirely in-memory without database services.

## 4. Architectural Boundaries
Each business module is partitioned into four Clean Architecture rings:
- **Domain**: Contains pure business logic, models, value objects, domain events, domain exceptions, and repository interfaces. Has no dependency on Spring or infrastructure.
- **Application**: Implements use cases, input/output ports, commands, queries, DTOs, and mappers.
- **Infrastructure**: Contains persistence adapters, Spring Data interfaces, external HTTP/messaging integrations, and configuration beans.
- **Presentation**: REST controllers validating incoming payloads and delegating to application input ports.

## 5. Maintenance
Maintained by the Backend Architecture Team. Updates occur whenever platform capabilities evolve.
