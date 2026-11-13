# Database Architecture & Migration Strategy

## 1. Purpose
This document details the database architecture roadmap, isolation principles, and migration standards for BazaarHub.

---

## 2. Phase 1 Constraint: Database-Free Operation
In strict compliance with Phase 1 requirements:
- **No Database Configured**: The application does not connect to any database.
- **Zero Database Dependencies**: No JPA, Hibernate, MariaDB, PostgreSQL, Redis, or Flyway dependencies exist in `pom.xml`.
- **In-Memory Readiness**: The backend compiles, boots, and passes all test suites completely in-memory.

---

## 3. Database Isolation Strategy for Modular Monolith (Phase 2+)
When persistence is introduced in subsequent phases, the database architecture will adhere to the following principles:

1. **Schema-Per-Module Isolation**:
   - Each business module owns a dedicated database schema (e.g., `schema_identity`, `schema_catalog`, `schema_order`) or logically partitioned tables with strict prefixing (`ord_orders`, `cat_products`, `inv_stock`).
2. **Zero Cross-Module SQL Joins**:
   - Queries cannot join tables across module boundaries. For example, the `order` module cannot execute a SQL join on `cat_products` or `cust_customers`.
3. **Foreign Key Rule**:
   - Primary data integrity inside a module is enforced with local foreign keys. Across modules, foreign keys are treated as soft references (storing only immutable UUIDs/IDs), preserving modular extractability.
4. **Flyway Versioned Migrations**:
   - Database migrations will be versioned per module under `src/main/resources/db/migration/` (e.g., `V1__init_identity.sql`, `V2__init_catalog.sql`).

---

## 4. Current Status
`src/main/resources/db/migration/` directory is prepared and preserved clean without migration scripts.
