# BazaarHub – Phase 15B Schema Migration Proposal
## Support Tickets Category Column & Customer Support Subsystem Audit
**Document Path:** `docs/implementation/PHASE_15B_SCHEMA_PROPOSAL.md`  
**Author:** AI Engineering Agent (Antigravity)  
**Date:** 2026-11-15  
**Target Environment:** Spring Boot 4.1.2 / Java 21 / MariaDB 11.8.9 (`u205680228_bazaarhub_db`)  
**Status:** **PROPOSED – AWAITING EXPLICIT USER APPROVAL BEFORE EXECUTION**

---

## 1. Executive Summary

This proposal presents the finalized schema migration plan for **Phase 15B – Customer Support & Ticket Management**.

Following the explicit rejection of subject-prefix encoding (an anti-pattern that violates First Normal Form and prevents performant filtering and reporting), this proposal specifies the addition of a dedicated `category` column to the existing `support_tickets` table along with a composite index on `(category, status)`.

In compliance with project safety rules:
- **Zero SQL has been executed.** The live database remains unmodified.
- The live database schema, indexes, foreign keys, and constraints were thoroughly audited.
- The current row count in `support_tickets` and `support_messages` is **0**, ensuring **zero data risk, zero downtime, and instant execution** once approved.

---

## 2. Live Database Inspection Findings

An audit of the live MariaDB 11.8.9 database (`u205680228_bazaarhub_db`) confirmed the following exact table definitions:

### 2.1 Table: `support_tickets`
```sql
CREATE TABLE `support_tickets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ticket_number` varchar(60) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `order_id` bigint(20) DEFAULT NULL,
  `subject` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `priority` enum('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM',
  `status` enum('OPEN','IN_PROGRESS','WAITING_FOR_CUSTOMER','RESOLVED','CLOSED') NOT NULL DEFAULT 'OPEN',
  `assigned_to` bigint(20) DEFAULT NULL,
  `resolved_at` datetime(3) DEFAULT NULL,
  `closed_at` datetime(3) DEFAULT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated_at` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_support_tickets_number` (`ticket_number`),
  KEY `fk_support_tickets_created_by` (`created_by`),
  KEY `fk_support_tickets_updated_by` (`updated_by`),
  KEY `idx_support_tickets_user` (`user_id`),
  KEY `idx_support_tickets_order` (`order_id`),
  KEY `idx_support_tickets_status` (`status`),
  KEY `idx_support_tickets_assigned` (`assigned_to`),
  KEY `idx_support_tickets_priority` (`priority`),
  CONSTRAINT `fk_support_tickets_assigned_to` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_support_tickets_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_support_tickets_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_support_tickets_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_support_tickets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2 Table: `support_messages`
```sql
CREATE TABLE `support_messages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `message` text NOT NULL,
  `attachment_url` varchar(500) DEFAULT NULL,
  `is_internal` tinyint(1) NOT NULL DEFAULT 0,
  `created_by` bigint(20) DEFAULT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated_at` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  KEY `fk_support_messages_created_by` (`created_by`),
  KEY `fk_support_messages_updated_by` (`updated_by`),
  KEY `idx_support_messages_ticket` (`ticket_id`),
  KEY `idx_support_messages_sender` (`sender_id`),
  KEY `idx_support_messages_created_at` (`created_at`),
  CONSTRAINT `fk_support_messages_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_support_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_support_messages_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `support_tickets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_support_messages_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.3 Row Count & Data Audit
- `SELECT COUNT(*) FROM support_tickets;` &rarr; **0 rows**
- `SELECT COUNT(*) FROM support_messages;` &rarr; **0 rows**

---

## 3. Final Proposed Schema Changes

### 3.1 Column Specification
- **Column Name:** `category`
- **Data Type:** `VARCHAR(50)`
- **Nullability:** `NOT NULL`
- **Default Value:** `'OTHER'`
- **Physical Position:** After `order_id` (immediately following contextual references and preceding customer-facing text `subject`).

### 3.2 Standard Category Values
The column stores standard uppercase business category identifiers:
1. `ORDER_ISSUE`: Order placement, cancellation, item modifications, order delays.
2. `PAYMENT_ISSUE`: Razorpay payment failures, double charges, UPI verification.
3. `REFUND_ISSUE`: Refund processing delays, partial refunds, bank credit status.
4. `DELIVERY_ISSUE`: Shipping partner delays, wrong delivery address, missing package, OTP issues.
5. `PRODUCT_ISSUE`: Defective product, wrong item received, damaged goods, warranty claims.
6. `SELLER_ISSUE`: Vendor misconduct, non-responsive merchant, counterfeit reports.
7. `ACCOUNT_ISSUE`: Login difficulties, profile access, password reset, account verification.
8. `OTHER`: General inquiries and miscellaneous feedback.

### 3.3 Indexing Strategy
- **Index Name:** `idx_support_tickets_category_status`
- **Indexed Columns:** `(category, status)`
- **Rationale:** 
  1. Administrative helpdesk dashboards filter tickets by category and status simultaneously (e.g. `WHERE category = 'PAYMENT_ISSUE' AND status IN ('OPEN', 'IN_PROGRESS')`).
  2. In MariaDB InnoDB B-Tree indexes, a composite index on `(category, status)` also serves queries filtering on `category` alone as the leftmost prefix.
  3. Complements the pre-existing index `idx_support_tickets_status (status)`.

---

## 4. DDL Migration Script

The proposed non-destructive DDL migration script to be executed upon user approval:

```sql
-- ============================================================================
-- BazaarHub Multi-Vendor Marketplace - Phase 15B Schema Migration
-- Table: support_tickets
-- Purpose: Add dedicated category column and composite index for ticket triage
-- Safety: Non-destructive, backward-compatible, default provided
-- Target: MariaDB 11.8.9 (u205680228_bazaarhub_db)
-- ============================================================================

ALTER TABLE `support_tickets`
  ADD COLUMN `category` VARCHAR(50) NOT NULL DEFAULT 'OTHER' AFTER `order_id`,
  ADD INDEX `idx_support_tickets_category_status` (`category`, `status`);
```

### 4.1 Verification Query
After applying the migration, verify with:
```sql
SHOW COLUMNS FROM `support_tickets` LIKE 'category';
SHOW INDEX FROM `support_tickets` WHERE Key_name = 'idx_support_tickets_category_status';
```

---

## 5. Impact on Existing Data & Operations

1. **Zero Data Loss**: Current row count in production is **0**. No existing rows require migration or backfilling.
2. **Backward Compatibility**: If rows existed, `DEFAULT 'OTHER'` ensures all historical records automatically receive a valid, non-null category without data distortion.
3. **Hibernate Validation**: Once applied, Spring Boot's `spring.jpa.hibernate.ddl-auto=validate` will validate `category` without conflict against `SupportTicketEntity`.
4. **Clean Architecture / Java Domain Model**:
   - In Java domain: mapped to enum `TicketCategory`:
     ```java
     public enum TicketCategory {
         ORDER_ISSUE,
         PAYMENT_ISSUE,
         REFUND_ISSUE,
         DELIVERY_ISSUE,
         PRODUCT_ISSUE,
         SELLER_ISSUE,
         ACCOUNT_ISSUE,
         OTHER
     }
     ```
   - In JPA entity:
     ```java
     @Enumerated(EnumType.STRING)
     @Column(name = "category", nullable = false, length = 50)
     @Builder.Default
     private TicketCategory category = TicketCategory.OTHER;
     ```

---

## 6. Additional Schema Gaps & Architecture Analysis

During our comprehensive audit of `support_tickets` and `support_messages`, we evaluated whether additional fields or tables are required for Phase 15B:

| Potential Field / Gap | Present in DB? | Analysis & Recommendation |
| :--- | :--- | :--- |
| **`category`** | **NO** | **Critical Gap**: Addressed by this proposal. Essential for ticket triage, SLA routing, and categorization. |
| **`seller_id`** | **NO** | **Minor Design Gap**: `support_tickets` currently links to `order_id` (customer parent order). In multi-vendor marketplaces, dispute tickets often target a specific merchant. However, because tickets can reference `order_id` (which links to `seller_orders`), the seller can be deduced contextually. Adding `seller_id` is **not required for Phase 15B MVP**, preserving schema stability. |
| **`attachment_url`** | **YES** | Already present in `support_messages.attachment_url VARCHAR(500)`. Supports Cloudinary uploaded files/images. |
| **`is_internal`** | **YES** | Already present in `support_messages.is_internal TINYINT(1) DEFAULT 0`. Enables support staff to record private staff notes invisible to customers. |
| **`closed_at` / `resolved_at`** | **YES** | Already present as `datetime(3)` columns on `support_tickets`. Captures precise SLA resolution timestamps. |
| **`assigned_to`** | **YES** | Already present as foreign key to `users(id)` with `ON DELETE SET NULL`. Enables support agent assignment. |
| **`priority`** | **YES** | Already present as `enum('LOW','MEDIUM','HIGH','URGENT') DEFAULT 'MEDIUM'`. |
| **`status`** | **YES** | Already present as `enum('OPEN','IN_PROGRESS','WAITING_FOR_CUSTOMER','RESOLVED','CLOSED') DEFAULT 'OPEN'`. |
| **CSAT Feedback / Rating** | **NO** | Optional post-resolution rating (`1-5` stars). Not strictly necessary for Phase 15B ticketing operations; can be addressed in future customer satisfaction enhancements. |

**Conclusion on Schema Completeness:**  
With the addition of `category VARCHAR(50) NOT NULL DEFAULT 'OTHER'` and index `idx_support_tickets_category_status`, the `support_tickets` and `support_messages` schema is **100% complete and sufficient** to implement all required capabilities of Phase 15B (customer ticketing, agent replies, internal notes, resolution flows, attachment uploads, and administrative filtering).

---

## 7. Migration Safety & Execution Plan

| Step | Action | Execution State |
| :--- | :--- | :--- |
| **Step 1** | Audit live tables, rows, keys, and constraints | **COMPLETED** (0 rows found) |
| **Step 2** | Draft non-destructive DDL migration script | **COMPLETED** (Documented in Section 4) |
| **Step 3** | User Review and Approval | **PENDING USER APPROVAL** |
| **Step 4** | Execute DDL against MariaDB production database | **HOLD (Awaiting user approval)** |
| **Step 5** | Verify Hibernate DDL validation & implement Phase 15B entities | **HOLD (Awaiting Step 4)** |

---

*This document is stored at `docs/implementation/PHASE_15B_SCHEMA_PROPOSAL.md`.*
