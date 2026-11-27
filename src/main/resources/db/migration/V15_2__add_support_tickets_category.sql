-- ============================================================================
-- BazaarHub Multi-Vendor Marketplace - Database Migration V15_2
-- Module       : Phase 15B – Customer Support & Ticket Management
-- Target Table : support_tickets
-- Target DB    : MariaDB 11.8.9+ / MySQL 8.0+
-- Description  : Add dedicated category column and composite index for ticket triage
-- Safety       : Fully idempotent (safe against duplicate execution), non-destructive
-- ============================================================================

-- 1. Safely add 'category' column if not already present
ALTER TABLE `support_tickets`
  ADD COLUMN IF NOT EXISTS `category` VARCHAR(50) NOT NULL DEFAULT 'OTHER' AFTER `order_id`;

-- 2. Safely add composite index on (category, status) if not already present
CREATE INDEX IF NOT EXISTS `idx_support_tickets_category_status`
  ON `support_tickets` (`category`, `status`);
