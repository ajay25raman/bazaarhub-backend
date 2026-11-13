# ADR-004: Absolute Domain Layer Independence from Spring Framework

## Status
Accepted

## Context
A critical vulnerability in enterprise Spring Boot systems is the viral spread of framework annotations (`@Component`, `@Service`, `@Autowired`, `@Transactional`, `@Entity`) directly into domain models and domain services. When domain logic depends on framework abstractions:
1. Domain models cannot be instantiated without framework runtime or reflection.
2. Business rules become coupled to database persistence lifecycles (lazy loading exceptions, Hibernate dirty checking anomalies).
3. Unit testing requires framework test harnesses or reflection agents, multiplying test execution time from milliseconds to seconds.
4. Upgrading Spring versions or migrating libraries risks breaking business logic.

## Problem
How do we ensure that BazaarHub's business rules, calculations, and domain state machines remain completely untainted by external frameworks and libraries?

## Decision
Enforce **Absolute Domain Independence**:
1. Classes in `com.bazaarhub.backend.<module>.domain.*` must NOT import any package from `org.springframework.*`, `jakarta.persistence.*`, or any infrastructure library.
2. Domain classes may only use pure Java SE 21 features (Java standard library: `java.time`, `java.util`, `java.math`, etc.) and optional immutability helpers where necessary.
3. Domain services are plain old Java objects (POJOs), instantiated or configured through Application layer orchestrators or factory methods.
4. Repository abstractions in the domain layer (`domain/repository/*`) are pure Java interfaces defining domain query/save contracts (SPI), never extending `org.springframework.data.repository.Repository`.
5. Domain models must never contain JPA annotations (`@Entity`, `@Table`, `@Id`, `@Column`).

## Alternatives Considered
- **Pragmatic Annotation Leakage (allowing `@Service` and `@Component` on Domain Services)**:
  - *Evaluation*: Rejected. Allowing `@Service` in the domain layer is the first step toward coupling domain classes to Spring context, leading developers to inject infrastructure beans directly into domain models.

## Advantages
- **Blazing Fast Unit Tests**: Domain unit tests run as pure Java tests executing thousands of assertions in sub-second intervals without Spring context startup.
- **Enterprise Longevity**: Core business calculations (pricing, commission, tax, discount, inventory reservation) can survive framework upgrades or transitions without a single line of modification.
- **Zero Framework Side Effects**: Eliminates Hibernate proxy surprises, detached entity issues, and implicit transaction flushes during business calculation execution.

## Disadvantages
- Requires explicit wiring in the application/infrastructure layer (or via `@Configuration` beans in `infrastructure/config`) to expose domain services as Spring beans where needed.
- Requires maintaining domain models separate from persistence entities, necessitating mapping logic.

## Consequences
- Architecture rule checks (e.g., ArchUnit in testing phases) will fail the build if any `org.springframework` package is imported inside a `domain` package.
- All domain validations throw domain-specific exceptions extending `com.bazaarhub.backend.shared.domain.exception.DomainException`.
