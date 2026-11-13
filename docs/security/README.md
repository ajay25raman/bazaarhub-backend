# Security Architecture & Policies

## 1. Purpose
This document outlines the security architecture, authentication standards, authorization models, and compliance policies governing BazaarHub.

---

## 2. Security Foundation
- **Framework**: Spring Security (`spring-boot-starter-security`).
- **Authentication Model**: Stateless token-based authentication using JSON Web Tokens (JWT) with RSA/ECDSA asymmetric signatures.
- **Role-Based Access Control (RBAC)**:
  - `ROLE_CUSTOMER`: Access to buyer profile, cart, order placement, reviews.
  - `ROLE_SELLER`: Access to vendor dashboard, store management, inventory, product submissions.
  - `ROLE_DELIVERY`: Access to assigned delivery orders, tracking updates, POD verification.
  - `ROLE_ADMIN`: Access to platform governance, seller approvals, audit logs, dispute resolutions.

---

## 3. Defense-in-Depth Principles
1. **Zero Trust Inside the Perimeter**: Every request must be authenticated and authorized based on token claims.
2. **Input Validation**: All incoming REST payloads undergo strict Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Size`, `@Pattern`) in the presentation layer before reaching application ports.
3. **Sensitive Data Protection**: Passwords, tokens, and payment secrets are never logged or stored in plaintext. Password hashing uses modern memory-hard functions (Argon2id or BCrypt).
4. **CORS & CSRF**:
   - Cross-Origin Resource Sharing (CORS) configured strictly for registered Flutter mobile user-agents and the Admin Web frontend domain.
   - CSRF protection disabled strictly for stateless JWT-authenticated REST APIs.

---

## 4. Phase 1 Security State
Default Spring Security starter is active on the classpath. Development-mode credentials auto-generate during application test runs, ensuring secure defaults until custom security filters and user details managers are introduced in the Identity phase.
