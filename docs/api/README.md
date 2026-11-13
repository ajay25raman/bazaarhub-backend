# API Documentation & Contracts

## 1. Purpose
This section establishes the REST API architectural standards, payload conventions, error handling contracts, and versioning rules for the BazaarHub backend.

---

## 2. API Design Principles
1. **RESTful Resource URIs**: Plural nouns representing domain aggregates (`/api/v1/catalog/products`, `/api/v1/orders`).
2. **Explicit Versioning**: All public endpoints are prefixed with `/api/v1/`. Breaking changes mandate an increment to `/api/v2/`.
3. **Thin Controllers**: REST controllers never execute business rules or database operations directly. They validate requests and delegate immediately to Application Input Ports.
4. **Standardized Response Envelope**: All endpoints return uniform JSON payloads wrapped in `ApiResponse<T>`.

---

## 3. Standard Response Envelope Contract

### Success Response (`200 OK`, `201 Created`)
```json
{
  "success": true,
  "message": "Resource retrieved successfully",
  "data": {
    "id": "ord_1029384",
    "status": "CONFIRMED",
    "totalAmount": 149.99
  },
  "errors": null,
  "timestamp": "2026-11-13T07:15:00Z"
}
```

### Error Response (`400 Bad Request`, `404 Not Found`, `422 Unprocessable`)
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    "quantity must be greater than zero",
    "shippingAddressId is required"
  ],
  "timestamp": "2026-11-13T07:15:00Z"
}
```

---

## 4. Health & Actuator Endpoints
- **Platform Health Endpoint**: `GET /api/v1/health` (Maintained in [`HealthController`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/shared/presentation/rest/health/HealthController.java)).
- **Spring Actuator Health**: `GET /actuator/health` (Spring Boot Actuator liveness/readiness).
- **Spring Actuator Info**: `GET /actuator/info` (Build and metadata information).

---

## 5. Maintenance & Future Roadmap
- Phase 2 will introduce SpringDoc OpenAPI 3 (`swagger-ui`) for automated interactive documentation generation.
