# ADR-002: Clean Architecture and Hexagonal Ports & Adapters Pattern

## Status
Accepted

## Context
Enterprise marketplace software frequently suffers from technical debt when framework details (Spring annotations, JPA entity annotations, HTTP controllers) leak into core business logic. When frameworks change, database drivers upgrade, or new protocols (GraphQL, gRPC, WebSocket) are introduced, tightly-coupled business logic requires extensive, risky rewrites.

## Problem
How should each business module be structured internally to guarantee that enterprise domain rules remain pristine, testable in total isolation, and immune to framework or database changes?

## Decision
Adopt **Clean Architecture** (concentric rings with Ports and Adapters) inside every business module:
1. **Domain Layer (Center)**: Encapsulates pure domain logic, entities, value objects, domain services, domain events, domain exceptions, and repository abstractions (SPI). Has ZERO external dependencies.
2. **Application Layer**: Contains use cases, input ports (use case interfaces), output ports (SPI), commands, queries, DTOs, and mappers. Depends only on the Domain layer.
3. **Infrastructure Layer**: Implements output ports. Houses persistence entities, database repositories, messaging adapters, cloud SDK integrations, and Spring `@Configuration` beans.
4. **Presentation Layer**: Exposes REST controllers, HTTP request/response DTOs, and validation logic. Calls application input ports.
5. **Dependency Inversion Principle**: The source code dependency arrow always points inwards:
   - `Presentation -> Application -> Domain`
   - `Infrastructure -> Application / Domain Abstractions`

## Alternatives Considered
- **Traditional 3-Tier Layered Architecture (`Controller -> Service -> DAO`)**:
  - *Evaluation*: Rejected. In traditional 3-tier architecture, domain objects are typically anemic data structures mapped directly to JPA tables. Business logic sits inside heavyweight Spring services tightly coupled to Hibernate and Spring Framework.
- **Onion Architecture**:
  - *Evaluation*: Evaluated favorably. Clean Architecture was selected as the concrete pattern because its explicit Ports & Adapters formulation provides cleaner decoupling for REST and messaging inputs/outputs.

## Advantages
- **Framework Independence**: The core domain logic does not depend on Spring Framework, Jakarta EE, or any third-party library.
- **Fast, Isolated Unit Testing**: Domain entities and use cases can be unit-tested with pure JUnit 5 in milliseconds without loading the Spring application context or mocking Spring beans.
- **Pluggable Infrastructure**: Swapping persistence mechanisms (e.g., from in-memory to PostgreSQL or MariaDB) requires zero changes to domain and application layers.
- **Thin Controllers**: REST controllers are strictly translation adapters that parse HTTP, validate DTOs, and invoke application ports.

## Disadvantages
- **Increased File Count**: Requires DTOs, domain models, entity models, and mappers rather than passing a single entity across all layers.
- **Boilerplate Mapping**: Mapping between HTTP requests, application commands, domain models, and persistence entities requires disciplined mapping logic.

## Consequences
- Every module must maintain strict 4-layer isolation (`domain`, `application`, `infrastructure`, `presentation`).
- Developers cannot annotate domain models with `@Entity`, `@Table`, or `@Component`.
- Tests for domain and application layers run as fast, standalone unit tests without Spring runner overhead.
