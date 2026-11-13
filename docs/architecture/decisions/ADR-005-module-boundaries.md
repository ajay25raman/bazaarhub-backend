# ADR-005: Strict Module Boundaries & Cross-Module Communication Governance

## Status
Accepted

## Context
In a Modular Monolith with 20 business modules, without explicit boundary governance, developers will naturally create direct couplings: injecting `OrderRepository` into `ReviewController`, querying `CustomerEntity` directly from `PaymentService`, or joining across module tables. Over time, the modular monolith degenerates into an untangled distributed monolith without the benefits of either architecture.

## Problem
How should module boundaries be defined and enforced in BazaarHub so that modules remain autonomous, decoupled, and safe from unintended cross-domain coupling?

## Decision
Establish **Strict Module Boundaries and Public Contract Governance**:
1. **Private by Default**: All classes inside `<module>/infrastructure/*`, `<module>/presentation/*`, and `<module>/domain/*` are internal to that module.
2. **Public Communication Ports**: When Module A needs data or actions from Module B, Module A must interact strictly through:
   - Module B's **Application Input Port** (public interface in `<moduleB>/application/port/input/*`), or
   - An asynchronous **Domain Event** published by Module B and subscribed to by Module A.
3. **No Cross-Module Repository or Persistence Access**: Module A must NEVER inject or reference Module B's repository, database entity, or persistence mapper.
4. **No Cross-Module Database Table Joins**: Each module conceptually owns its schema and tables. No cross-schema or cross-table SQL joins are permitted.
5. **No Circular Dependencies**: Cyclic dependencies between modules (e.g., Order -> Payment -> Order) are strictly prohibited. Circular communication must be resolved via event choreography or mediator ports.
6. **Shared Kernel Boundaries**: `com.bazaarhub.backend.shared` contains only generic technical infrastructure, common response envelopes, and universal domain exceptions. No business entities or domain rules are allowed in shared.

## Alternatives Considered
- **Direct Java Injections Across Modules**:
  - *Evaluation*: Rejected. Allowing services to inject arbitrary components from other modules creates an unmanageable web of dependencies and makes testing individual modules in isolation impossible.
- **REST / HTTP Internal Communication Between Modules**:
  - *Evaluation*: Rejected. Running HTTP requests between modules within the same JVM introduces needless serialization, latency, and failure points without any architectural benefit over in-memory Java interfaces.

## Advantages
- **Autonomous Evolution**: A module's internal domain logic, persistence schema, or internal algorithms can be completely refactored without breaking any other module.
- **High Testability**: Modules can be tested by mocking only their public input/output ports rather than deep internal dependencies.
- **Microservice Readiness**: Because modules share no internal code, database tables, or circular dependencies, any module can be extracted into an independent microservice if needed.

## Disadvantages
- Requires defining explicit Port interfaces and DTOs for cross-module communication instead of direct internal object access.

## Consequences
- Cross-module operations (such as order checkout orchestrating inventory, payment, and notifications) must use explicit application ports and event publishers.
- Code reviews must reject any PR where a class in module X imports an internal class from module Y.
