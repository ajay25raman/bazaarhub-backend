-- ============================================================================
-- BazaarHub Multi-Vendor Marketplace - Phase 5 Inventory Permissions Audit
-- ============================================================================
-- Target Database : MariaDB 11.8.9 (u205680228_bazaarhub_db)
-- Target Tables   : permissions, role_permissions
-- Safety Level    : Non-destructive, Idempotent, Reviewable, Read-Only Audit
-- Strategy        : INSERT IGNORE with dynamic role/permission key joins
-- Note            : DO NOT execute against production without change-window authorization.
-- ============================================================================

START TRANSACTION;

-- 1. Ensure inventory permissions exist
INSERT IGNORE INTO `permissions` (`name`, `description`, `created_at`, `updated_at`) VALUES
  ('INVENTORY_READ', 'View current stock counts, reservations, and SKU thresholds for own store', NOW(3), NOW(3)),
  ('INVENTORY_UPDATE', 'Adjust stock levels, restock SKUs, and manage warehouse availability for own products', NOW(3), NOW(3)),
  ('INVENTORY_AUDIT', 'Inspect platform-wide stock logs, inventory audit trails, and discrepancy records', NOW(3), NOW(3));

-- 2. Ensure mappings to ROLE_SELLER (INVENTORY_READ, INVENTORY_UPDATE)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.name IN ('INVENTORY_READ', 'INVENTORY_UPDATE')
WHERE r.name = 'ROLE_SELLER';

-- 3. Ensure mappings to ROLE_ADMIN (INVENTORY_READ, INVENTORY_AUDIT)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.name IN ('INVENTORY_READ', 'INVENTORY_AUDIT')
WHERE r.name = 'ROLE_ADMIN';

COMMIT;
