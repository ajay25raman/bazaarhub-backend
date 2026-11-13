# ADR-001: Modular Monolith Architecture over Distributed Microservices

## Status
Accepted

## Context
BazaarHub is an enterprise AI-powered multi-vendor e-commerce marketplace supporting four client interfaces (Customer, Seller, Delivery Boy, Admin). The platform requires rapid feature iteration, strict transactional integrity across financial checkout sagas, high operational velocity, and low operational overhead during initial growth and expansion phases.

Distributed microservices architectures introduce severe operational tax: network latency, distributed consensus (2PC/Sagas), complex service meshes, observability overhead (distributed tracing, log aggregation), eventual consistency edge cases, and high deployment infrastructure costs.

## Problem
How should BazaarHub's backend architecture be organized to guarantee domain modularity, clean module isolation, and maintainability without incurring premature microservice complexity and operational failure modes?

## Decision
Adopt a **Modular Monolith** architecture for BazaarHub:
1. Deploy the backend as a unified, single deployment artifact (JAR) running in a single JVM process.
2. Structure the internal codebase into 20 strictly encapsulated business modules (`identity`, `customer`, `catalog`, `order`, `payment`, etc.).
3. Enforce strict boundaries between modules using Java package encapsulation, public application ports, and event-driven decoupling.
4. Prevent direct cross-module database table joins or foreign key traversal across module boundaries.

## Alternatives Considered
- **Distributed Microservices**:
  - *Evaluation*: Rejected due to premature complexity. Managing 20 independent microservices, Kubernetes clusters, API gateways, and distributed transaction rollbacks at this stage would drastically slow down feature development and inflate infrastructure costs.
- **Traditional Layered Monolith ("Big Ball of Mud")**:
  - *Evaluation*: Rejected. Grouping code by technical layers (`controllers/`, `services/`, `repositories/`) inevitably leads to spaghetti code, circular dependencies, and tangled domain logic where any service can query any repository.

## Advantages
- **Single Deployment Pipeline**: Trivial deployment, continuous integration, and rollbacks.
- **Transactional Simplicity**: In-process method calls eliminate network failure modes, RPC serialization overhead, and partial network partitions.
- **High Developer Ergonomics**: Single project checkout, single IDE window, rapid local startup, and immediate refactoring capabilities.
- **Zero Network Latency**: Cross-module queries execute via in-memory Java method invocations rather than HTTP/gRPC.
- **Effortless Future Extraction**: Because module boundaries and data models are strictly isolated, any module can be extracted into an independent microservice in the future if specific scaling bottlenecks warrant it.

## Disadvantages
- **Shared Process Failure Domain**: An unhandled OutOfMemoryError or CPU-intensive thread in one module can impact the entire JVM process.
- **Single Technology Stack**: All modules must conform to Java 21 and the unified Maven build lifecycle.
- **Horizontal Scaling Granularity**: The entire monolith must be scaled horizontally rather than scaling individual high-traffic modules (e.g., catalog search) independently.

## Consequences
- All developers must respect module encapsulation rules. Cross-module calls must only use application ports and domain events.
- Unit and integration tests can run entirely in-memory within seconds.
- The build produces a single self-contained executable JAR for production.
