# Testing Strategy & Guidelines

## 1. Purpose
This document defines testing conventions, execution standards, and quality gates for the BazaarHub backend.

---

## 2. Testing Pyramid & Principles

```
            / \
           /   \         E2E / System Tests (Full monolith flow)
          /-----\
         /       \       Slice & Integration Tests (@SpringBootTest, MockMvc)
        /---------\
       /           \     Unit Tests (Pure JUnit 5 & Mockito - Domain & Application)
      /-------------\
```

### 1. Pure Unit Tests (Base of Pyramid - 70%+)
- **Scope**: Domain models, Value Objects, Domain Services, Use Cases, Mappers.
- **Rules**: Zero Spring context loading. Executed via plain JUnit 5 and Mockito. Fast, isolated, reliable (sub-second execution).

### 2. Slice Tests (Middle of Pyramid - 20%)
- **Scope**: REST controllers (`@WebMvcTest`), validation rules, response serialization.
- **Rules**: Mock the Application Input Ports using `@MockitoBean` / `@MockBean`. Test HTTP status codes, validation constraints, and error envelopes.

### 3. Context & Integration Tests (Top of Pyramid - 10%)
- **Scope**: Full Spring context startup (`@SpringBootTest`), application bootstrap, security filter chains.

---

## 3. Test Structure Conventions
The test package tree strictly mirrors the production code layout under `src/test/java/com/bazaarhub/backend/`:
```
src/test/java/com/bazaarhub/backend/
├── BazaarhubBackendApplicationTests.java
├── shared/
├── identity/
├── customer/
├── seller/
├── store/
├── catalog/
├── inventory/
├── cart/
├── wishlist/
├── order/
├── payment/
├── refund/
├── delivery/
├── commission/
├── finance/
├── promotion/
├── review/
├── notification/
├── support/
├── admin/
└── ai/
```

---

## 4. Running Tests
```bash
# Execute entire test suite
./mvnw test

# Execute single test class
./mvnw test -Dtest=BazaarhubBackendApplicationTests
```
