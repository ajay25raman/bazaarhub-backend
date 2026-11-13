# ADR-003: Package-by-Feature Modular Organization over Layer-First Structure

## Status
Accepted

## Context
Java projects typically choose between two packaging paradigms:
1. **Package-by-Layer**: Top-level packages are organized by technical layers:
   ```
   com.bazaarhub.backend.controllers
   com.bazaarhub.backend.services
   com.bazaarhub.backend.repositories
   com.bazaarhub.backend.models
   ```
2. **Package-by-Feature (Package-by-Component/Module)**: Top-level packages are organized by cohesive business domains:
   ```
   com.bazaarhub.backend.identity
   com.bazaarhub.backend.catalog
   com.bazaarhub.backend.order
   ```

## Problem
How should the root package `com.bazaarhub.backend` organize its 20 business domains to ensure high cohesive grouping, clear code ownership, and enforce architectural boundaries?

## Decision
Adopt **Package-by-Feature (Package-by-Module)** as the root organizational structure for BazaarHub:
1. The top-level packages directly under `com.bazaarhub.backend` represent the 20 distinct business modules:
   `identity`, `customer`, `seller`, `store`, `catalog`, `inventory`, `cart`, `wishlist`, `order`, `payment`, `refund`, `delivery`, `commission`, `finance`, `promotion`, `review`, `notification`, `support`, `admin`, `ai`.
2. Cross-cutting shared utilities and common response envelopes reside in `com.bazaarhub.backend.shared`.
3. Application startup and composition root reside in `com.bazaarhub.backend.bootstrap`.
4. Inside each business module package, Clean Architecture layers (`domain`, `application`, `infrastructure`, `presentation`) are nested.

## Alternatives Considered
- **Package-by-Layer**:
  - *Evaluation*: Rejected. In a 20-module enterprise application, placing all controllers in `com.bazaarhub.backend.controllers` and all services in `com.bazaarhub.backend.services` creates catastrophic coupling, prevents modular encapsulation, makes discovering related code painful, and eliminates any ability to extract modules in the future.
- **Maven Multi-Module Build**:
  - *Evaluation*: Evaluated for Phase 2. In Phase 1, managing 20 separate `pom.xml` submodules introduces unnecessary build complexity and slower build cycles. A single Maven project with package-by-feature modularization provides identical boundary guarantees with zero build overhead.

## Advantages
- **High Cohesion**: All code related to an e-commerce feature (e.g., `catalog`) is located together in one package tree.
- **Cognitive Clarity**: Developers working on "inventory" only navigate the `inventory/` package without scrolling through hundreds of unrelated controllers.
- **Clear Code Ownership**: Teams can be assigned ownership of specific feature packages without conflicting on shared layer directories.
- **Trivial Modular Refactoring**: Extracting a module into an independent service or Maven submodule in the future is as simple as copying that single package.

## Disadvantages
- Developers unfamiliar with Clean Architecture must understand that layers repeat inside each feature package rather than existing globally.

## Consequences
- Every new business capability must find its home in an existing feature module or propose a new business module.
- Generic classes must not be dumped into a global namespace.
