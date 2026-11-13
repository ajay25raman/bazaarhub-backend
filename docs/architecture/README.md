# Architecture Documentation

## 1. Purpose
This section documents the macro and micro architecture of the BazaarHub backend, providing clear guidance on architectural patterns, constraints, and layer boundaries.

## 2. Directory Contents
- [`overview/`](file:///Users/ajaykumar/bazaarhub-backend/docs/architecture/overview/README.md): High-level system overview, technology stack rationale, and modular monolith structure.
- [`decisions/`](file:///Users/ajaykumar/bazaarhub-backend/docs/architecture/decisions/README.md): Formal Architecture Decision Records (ADRs).
- [`diagrams/`](file:///Users/ajaykumar/bazaarhub-backend/docs/architecture/diagrams/README.md): Mermaid architecture diagrams, dependency flow diagrams, and layer interaction models.

## 3. Core Architectural Tenets
1. **Modular Monolith**: High cohesion within modules, loose coupling between modules.
2. **Clean Architecture (Hexagonal / Ports and Adapters)**: Business logic in the center, infrastructure on the periphery.
3. **Dependency Inversion Principle (DIP)**: High-level modules do not depend on low-level modules; both depend on abstractions.
4. **Domain Independence**: Domain models and services do not import Spring Framework annotations or classes.
5. **Single Ingress & Composition Root**: `com.bazaarhub.backend` houses the unified application entrypoint.

## 4. Maintenance & Governance
All architectural amendments require updating corresponding overview documents and submitting an ADR for peer review before code changes are merged.

## 5. Current Status
Phase 1 architectural foundation established, verified, and compiling cleanly under Java 21 and Spring Boot 4.1.2.
