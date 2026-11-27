# BazaarHub – Phase 15B Implementation Report
## Customer Support & Ticket Management Module

---

### Executive Summary

Phase 15B delivers the **Customer Support & Ticket Management** module for **BazaarHub**, an enterprise-grade AI-powered multi-vendor marketplace. This phase provides multi-role support ticket capabilities for **Customers**, **Sellers**, **Delivery Partners**, and **Admin/Helpdesk Agents**. It features strict data ownership validation, private internal staff notes segregation, ticket assignment workflows, Cloudinary attachment support, and non-blocking notification dispatch integrated with Phase 15A.

All changes were implemented following Clean Architecture and SOLID principles, preserving existing schema constraints without modifying previous migrations or executing production DDL on the live Hostinger database (`u205680228_bazaarhub_db`).

---

### 1. Database Migration & Schema Verification

#### 1.1 Migration File
- **Path**: [`src/main/resources/db/migration/V15_2__add_support_tickets_category.sql`](file:///Users/ajaykumar/bazaarhub-backend/src/main/resources/db/migration/V15_2__add_support_tickets_category.sql)
- **Safety**: Safe against duplicate execution via stored procedure checking `information_schema.COLUMNS` and `information_schema.STATISTICS`.
- **Target Table**: `support_tickets`
- **Added Column**:
  ```sql
  category VARCHAR(50) NOT NULL DEFAULT 'OTHER'
  ```
- **Added Composite Index**:
  ```sql
  idx_support_tickets_category_status (category, status)
  ```

#### 1.2 Status Enum & Lifecycle Verification
An audit of the MariaDB table structure confirmed the exact existing `status` enum definition:
```sql
enum('OPEN','IN_PROGRESS','WAITING_FOR_CUSTOMER','RESOLVED','CLOSED') NOT NULL DEFAULT 'OPEN'
```
- **Assignment Compatibility**: The live database enum **does not** contain an `ASSIGNED` value. In accordance with zero-schema-drift guidelines, ticket assignment is tracked via the existing foreign key `assigned_to` (`UserEntity assignedTo`), and newly assigned tickets transition naturally from `OPEN` to `IN_PROGRESS`.
- **Supported Categories**:
  1. `ORDER_ISSUE`
  2. `PAYMENT_ISSUE`
  3. `REFUND_ISSUE`
  4. `DELIVERY_ISSUE`
  5. `PRODUCT_ISSUE`
  6. `SELLER_ISSUE`
  7. `ACCOUNT_ISSUE`
  8. `OTHER`

#### 1.3 Production Database Safety
- **Zero Production DDL Executed**: No migration SQL, DDL, or schema alterations were executed against the remote Hostinger MariaDB server (`srv664.hstgr.io`).
- **Validation**: Schema mapping and JPA entities were verified via in-memory MariaDB-compatible test suites and `SchemaValidationTest.java`.

---

### 2. Architecture & Component Structure

The implementation follows the modular DDD layered pattern adopted across BazaarHub:

```
src/main/java/com/bazaarhub/backend/support/
├── domain/
│   ├── model/
│   │   ├── TicketCategory.java       # 8 approved ticket categories
│   │   ├── TicketStatus.java         # 5 DB-aligned lifecycle statuses
│   │   └── TicketPriority.java       # LOW, MEDIUM, HIGH, URGENT
│   └── exception/
│       ├── SupportTicketNotFoundException.java
│       ├── UnauthorizedTicketAccessException.java
│       └── InvalidTicketOperationException.java
├── infrastructure/
│   └── persistence/
│       ├── entity/
│       │   ├── SupportTicketEntity.java    # Maps to support_tickets
│       │   └── SupportMessageEntity.java   # Maps to support_messages
│       └── repository/
│           ├── SpringDataSupportTicketRepository.java
│           └── SpringDataSupportMessageRepository.java
├── application/
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateTicketRequest.java
│   │   │   ├── AddTicketMessageRequest.java
│   │   │   ├── AssignTicketRequest.java
│   │   │   ├── UpdateTicketStatusRequest.java
│   │   │   └── UpdateTicketPriorityRequest.java
│   │   └── response/
│   │       ├── SupportTicketResponse.java
│   │       ├── SupportMessageResponse.java
│   │       └── TicketStatsResponse.java
│   ├── mapper/
│   │   ├── SupportTicketMapper.java
│   │   └── SupportMessageMapper.java
│   └── service/
│       ├── SupportTicketService.java
│       └── SupportTicketServiceImpl.java
└── presentation/
    └── rest/
        ├── CustomerSupportController.java    # /api/v1/customer/tickets/**
        ├── SellerSupportController.java      # /api/v1/seller/tickets/**
        ├── DeliverySupportController.java    # /api/v1/delivery/tickets/**
        └── AdminSupportController.java       # /api/v1/admin/support/tickets/**
```

---

### 3. REST API Surface

#### 3.1 Customer Support Endpoints (`/api/v1/customer/tickets`)
- `POST /api/v1/customer/tickets`: Create a new support ticket (Category, Subject, Description, Priority, optional Reference ID).
- `GET /api/v1/customer/tickets`: Paginated list of customer's tickets (filterable by status and category).
- `GET /api/v1/customer/tickets/{ticketId}`: View details of customer's own ticket.
- `POST /api/v1/customer/tickets/{ticketId}/messages`: Post reply to conversation.
- `GET /api/v1/customer/tickets/{ticketId}/messages`: View message thread (internal staff notes are strictly excluded).
- `POST /api/v1/customer/tickets/attachments`: Upload image/document attachment to Cloudinary (returns hosted secure URL).
- `PATCH /api/v1/customer/tickets/{ticketId}/close`: Customer self-close ticket.

#### 3.2 Seller Support Endpoints (`/api/v1/seller/tickets`)
- `POST /api/v1/seller/tickets`: Raise a seller inquiry (e.g., `PAYMENT_ISSUE`, `SELLER_ISSUE`, `ORDER_ISSUE`).
- `GET /api/v1/seller/tickets`: Paginated list of seller's tickets.
- `GET /api/v1/seller/tickets/{ticketId}`: View ticket details.
- `POST /api/v1/seller/tickets/{ticketId}/messages`: Reply to support agent.
- `GET /api/v1/seller/tickets/{ticketId}/messages`: View conversation messages (excluding internal notes).
- `PATCH /api/v1/seller/tickets/{ticketId}/close`: Close seller ticket.

#### 3.3 Delivery Partner Support Endpoints (`/api/v1/delivery/tickets`)
- `POST /api/v1/delivery/tickets`: Raise logistics/dispatch issue (`DELIVERY_ISSUE`, `ORDER_ISSUE`).
- `GET /api/v1/delivery/tickets`: List courier's active and historical tickets.
- `GET /api/v1/delivery/tickets/{ticketId}`: View delivery ticket details.
- `POST /api/v1/delivery/tickets/{ticketId}/messages`: Reply to support desk.
- `GET /api/v1/delivery/tickets/{ticketId}/messages`: View messages (excluding internal notes).
- `PATCH /api/v1/delivery/tickets/{ticketId}/close`: Close ticket.

#### 3.4 Admin / Helpdesk Endpoints (`/api/v1/admin/support/tickets`)
- `GET /api/v1/admin/support/tickets`: Global ticket queue with pagination, filtering by `status`, `category`, `priority`, and search term.
- `GET /api/v1/admin/support/tickets/{ticketId}`: Inspect full ticket context.
- `POST /api/v1/admin/support/tickets/{ticketId}/assign`: Assign or reassign ticket to a support agent.
- `PATCH /api/v1/admin/support/tickets/{ticketId}/status`: Update lifecycle status (`OPEN`, `IN_PROGRESS`, `WAITING_FOR_CUSTOMER`, `RESOLVED`, `CLOSED`).
- `PATCH /api/v1/admin/support/tickets/{ticketId}/priority`: Escalate/de-escalate priority (`LOW`, `MEDIUM`, `HIGH`, `URGENT`).
- `POST /api/v1/admin/support/tickets/{ticketId}/messages`: Post public response OR private internal note (`isInternal = true`).
- `GET /api/v1/admin/support/tickets/{ticketId}/messages`: Retrieve complete message history including internal audit notes.
- `GET /api/v1/admin/support/tickets/statistics`: Aggregated ticket metrics (open, in progress, waiting, resolved, closed, unassigned count).

---

### 4. Security & RBAC Enforcement

1. **Route Authorizations in [`SecurityConfig.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/bootstrap/config/SecurityConfig.java)**:
   - `/api/v1/customer/tickets/**` -> requires `ROLE_CUSTOMER`
   - `/api/v1/seller/tickets/**` -> requires `ROLE_SELLER`
   - `/api/v1/delivery/tickets/**` -> requires `ROLE_DELIVERY`
   - `/api/v1/admin/support/**` -> requires `ROLE_ADMIN`
2. **Data Ownership Validation**:
   - Every read and write method on customer, seller, and delivery endpoints checks `ticket.getUser().getId().equals(currentUserId)`.
   - Unauthorized attempts throw `UnauthorizedTicketAccessException`, mapped by `GlobalRestExceptionHandler` to `403 FORBIDDEN`.
3. **Internal Note Segregation**:
   - `support_messages.is_internal` (`TINYINT(1)`):
   - Non-staff queries invoke `findByTicketIdAndInternalFalseOrderByCreatedAtAsc`.
   - Only admin/support endpoints invoke `findByTicketIdOrderByCreatedAtAsc`.
   - If an unauthorized user attempts to set `isInternal = true`, the controllers sanitize the flag to `false`.
4. **Closed Ticket Immutability**:
   - Replies cannot be posted to tickets in `CLOSED` status, throwing `InvalidTicketOperationException` (`400 BAD REQUEST`).

---

### 5. Media & Attachment Storage (Cloudinary)

- File uploads are validated for allowed content types:
  - `image/jpeg`, `image/png`, `image/webp`, `application/pdf`
- Size cap enforced: **10 MB**.
- Integration: Reuses existing Cloudinary SDK credentials when configured (`cloudinary.cloud-name`, `cloudinary.api-key`, `cloudinary.api-secret`); otherwise produces fallback deterministic mock URLs for testing and local development (`https://res.cloudinary.com/bazaarhub/image/upload/v1/support/...`).

---

### 6. Phase 15A Notification Integration

Non-blocking notification integration dispatches events via `NotificationService`:
- `SUPPORT_TICKET_CREATED`: Sent to user upon ticket submission (`TKT-XXXXXXXX-XXXXXX`).
- `SUPPORT_MESSAGE_RECEIVED`: Sent to customer when support agent posts a public reply.
- `SYSTEM_ALERT`: Sent to customer when ticket status changes to `RESOLVED` or `CLOSED`.
- **Fault-Tolerant Execution**: All notifications are dispatched with exception containment (`try/catch`); notification delivery failures will **never** roll back ticket operations.

---

### 7. Verification & Test Execution Results

#### 7.1 Support Ticket Unit Tests
- **Class**: [`SupportTicketServiceUnitTest.java`](file:///Users/ajaykumar/bazaarhub-backend/src/test/java/com/bazaarhub/backend/support/SupportTicketServiceUnitTest.java)
- **Tests**: 11 executed, **11 passed**, 0 failures, 0 errors.
- **Coverage**:
  - Ticket creation with sequential ticket number generation.
  - Ownership validation and 403 authorization checks.
  - State transitions (`WAITING_FOR_CUSTOMER` -> `IN_PROGRESS` on customer reply).
  - Staff assignment and reassignment.
  - Status updates (`RESOLVED`, `CLOSED` with timestamps).
  - Internal staff notes vs public messages.
  - Exception handling for invalid status transitions on closed tickets.

#### 7.2 Support Ticket Integration Tests
- **Class**: [`SupportTicketIntegrationTest.java`](file:///Users/ajaykumar/bazaarhub-backend/src/test/java/com/bazaarhub/backend/support/SupportTicketIntegrationTest.java)
- **Tests**: 3 integration flows, **3 passed**, 0 failures, 0 errors.
- **Scenarios**:
  - `testCustomerSupportLifecycle`: Ticket creation, attachment upload, messaging, ownership isolation (User 2 blocked with 403), customer closure.
  - `testSellerAndDeliverySupportFlows`: Seller and courier endpoints, category filtering, unauthenticated & role-based restrictions.
  - `testAdminSupportDeskOperations`: Global queue filtering, ticket assignment, priority escalation, internal notes invisible to customer thread, resolution statistics.

#### 7.3 Full Regression Suite
- **Command**: `./mvnw test`
- **Result**:
  ```
  [INFO] Results:
  [INFO] Tests run: 454, Failures: 0, Errors: 0, Skipped: 3
  [INFO] BUILD SUCCESS
  [INFO] Total time: 03:31 min
  ```
- **Platform Integrity**: 100% test pass rate across all 15 modules with zero regressions.

---

### 8. Deliverables & Artifacts Summary

| Component | Path | Description |
| :--- | :--- | :--- |
| **Migration** | [`V15_2__add_support_tickets_category.sql`](file:///Users/ajaykumar/bazaarhub-backend/src/main/resources/db/migration/V15_2__add_support_tickets_category.sql) | Flyway idempotent migration for `support_tickets.category` and index |
| **Entities** | [`SupportTicketEntity.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/infrastructure/persistence/entity/SupportTicketEntity.java) | JPA entity for `support_tickets` |
| | [`SupportMessageEntity.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/infrastructure/persistence/entity/SupportMessageEntity.java) | JPA entity for `support_messages` |
| **Service** | [`SupportTicketServiceImpl.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/application/service/SupportTicketServiceImpl.java) | Business logic, state transitions, Cloudinary, notifications |
| **Controllers** | [`CustomerSupportController.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/presentation/rest/CustomerSupportController.java) | Customer-facing ticket management |
| | [`SellerSupportController.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/presentation/rest/SellerSupportController.java) | Seller-facing ticket management |
| | [`DeliverySupportController.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/presentation/rest/DeliverySupportController.java) | Delivery partner ticket management |
| | [`AdminSupportController.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/support/presentation/rest/AdminSupportController.java) | Admin helpdesk, assignment, internal notes, stats |
| **Security** | [`SecurityConfig.java`](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/bootstrap/config/SecurityConfig.java) | Route access controls for Phase 15B |
| **Report** | [`PHASE_15B_IMPLEMENTATION_REPORT.md`](file:///Users/ajaykumar/bazaarhub-backend/docs/implementation/PHASE_15B_IMPLEMENTATION_REPORT.md) | Full implementation audit and verification report |
