-- ============================================================================
-- BazaarHub Multi-Vendor Marketplace - Phase 1 RBAC Validation Script
-- ============================================================================
-- Target Database : MariaDB 11.8.9 (u205680228_bazaarhub_db)
-- Safety Level    : STRICTLY READ-ONLY (SELECT only)
-- Purpose         : Verify Role, Permission, and Mapping integrity after seed
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. VERIFY ALL 4 PLATFORM ROLES EXIST
-- Expected Count: Exactly 4 roles
-- ----------------------------------------------------------------------------
SELECT 
    id, 
    name, 
    description, 
    created_at, 
    updated_at
FROM `roles`
WHERE name IN ('ROLE_CUSTOMER', 'ROLE_SELLER', 'ROLE_DELIVERY', 'ROLE_ADMIN')
ORDER BY id ASC;

SELECT COUNT(*) AS total_roles_count 
FROM `roles`;

-- ----------------------------------------------------------------------------
-- 2. VERIFY PERMISSION COUNT AND IDENTIFY ANY MISSING EXPECTED PERMISSIONS
-- Expected Total: 68 distinct permissions
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS total_permissions_count 
FROM `permissions`;

-- Check if any expected permissions are missing from the permissions table
SELECT expected.name AS missing_permission_name
FROM (
    SELECT 'CUSTOMER_PROFILE_READ' AS name UNION ALL
    SELECT 'CUSTOMER_PROFILE_UPDATE' UNION ALL
    SELECT 'CUSTOMER_ACCOUNT_DELETE' UNION ALL
    SELECT 'CART_READ' UNION ALL
    SELECT 'CART_UPDATE' UNION ALL
    SELECT 'WISHLIST_READ' UNION ALL
    SELECT 'WISHLIST_UPDATE' UNION ALL
    SELECT 'SELLER_PROFILE_READ' UNION ALL
    SELECT 'SELLER_PROFILE_UPDATE' UNION ALL
    SELECT 'SELLER_ONBOARDING_SUBMIT' UNION ALL
    SELECT 'STORE_READ' UNION ALL
    SELECT 'STORE_UPDATE' UNION ALL
    SELECT 'PRODUCT_READ' UNION ALL
    SELECT 'PRODUCT_CREATE' UNION ALL
    SELECT 'PRODUCT_UPDATE' UNION ALL
    SELECT 'PRODUCT_DELETE' UNION ALL
    SELECT 'CATEGORY_READ' UNION ALL
    SELECT 'BRAND_READ' UNION ALL
    SELECT 'PRODUCT_MODERATE' UNION ALL
    SELECT 'CATEGORY_MANAGE' UNION ALL
    SELECT 'BRAND_MANAGE' UNION ALL
    SELECT 'INVENTORY_READ' UNION ALL
    SELECT 'INVENTORY_UPDATE' UNION ALL
    SELECT 'INVENTORY_AUDIT' UNION ALL
    SELECT 'ORDER_CREATE' UNION ALL
    SELECT 'ORDER_READ' UNION ALL
    SELECT 'ORDER_CANCEL' UNION ALL
    SELECT 'SELLER_ORDER_READ' UNION ALL
    SELECT 'SELLER_ORDER_UPDATE' UNION ALL
    SELECT 'DELIVERY_ORDER_READ' UNION ALL
    SELECT 'ORDER_MANAGE' UNION ALL
    SELECT 'DELIVERY_PROFILE_READ' UNION ALL
    SELECT 'DELIVERY_PROFILE_UPDATE' UNION ALL
    SELECT 'DELIVERY_ASSIGNMENT_READ' UNION ALL
    SELECT 'DELIVERY_STATUS_UPDATE' UNION ALL
    SELECT 'DELIVERY_LOCATION_UPDATE' UNION ALL
    SELECT 'DELIVERY_MANAGE' UNION ALL
    SELECT 'PAYMENT_CREATE' UNION ALL
    SELECT 'PAYMENT_READ' UNION ALL
    SELECT 'REFUND_REQUEST' UNION ALL
    SELECT 'SELLER_PAYMENT_READ' UNION ALL
    SELECT 'PAYMENT_MANAGE' UNION ALL
    SELECT 'REFUND_MANAGE' UNION ALL
    SELECT 'COMMISSION_READ' UNION ALL
    SELECT 'PAYOUT_READ' UNION ALL
    SELECT 'PAYOUT_REQUEST' UNION ALL
    SELECT 'COMMISSION_MANAGE' UNION ALL
    SELECT 'PAYOUT_MANAGE' UNION ALL
    SELECT 'REVIEW_CREATE' UNION ALL
    SELECT 'REVIEW_READ' UNION ALL
    SELECT 'REVIEW_UPDATE' UNION ALL
    SELECT 'REVIEW_DELETE' UNION ALL
    SELECT 'SELLER_REVIEW_REPLY' UNION ALL
    SELECT 'REVIEW_MODERATE' UNION ALL
    SELECT 'SUPPORT_TICKET_CREATE' UNION ALL
    SELECT 'SUPPORT_TICKET_READ' UNION ALL
    SELECT 'SUPPORT_TICKET_REPLY' UNION ALL
    SELECT 'SUPPORT_TICKET_MANAGE' UNION ALL
    SELECT 'USER_MANAGE' UNION ALL
    SELECT 'ROLE_MANAGE' UNION ALL
    SELECT 'SELLER_MANAGE' UNION ALL
    SELECT 'DELIVERY_PARTNER_MANAGE' UNION ALL
    SELECT 'PLATFORM_SETTINGS_MANAGE' UNION ALL
    SELECT 'AUDIT_LOG_READ' UNION ALL
    SELECT 'SELLER_ANALYTICS_READ' UNION ALL
    SELECT 'DELIVERY_ANALYTICS_READ' UNION ALL
    SELECT 'PLATFORM_ANALYTICS_READ' UNION ALL
    SELECT 'REPORT_EXPORT'
) AS expected
LEFT JOIN `permissions` p ON expected.name = p.name
WHERE p.id IS NULL;

-- ----------------------------------------------------------------------------
-- 3. VERIFY ROLE-PERMISSION MAPPING COUNTS
-- Expected Counts:
--   ROLE_ADMIN    : 34 permissions
--   ROLE_CUSTOMER : 23 permissions
--   ROLE_DELIVERY : 11 permissions
--   ROLE_SELLER   : 25 permissions
--   Grand Total   : 93 mappings
-- ----------------------------------------------------------------------------
SELECT 
    r.name AS role_name, 
    COUNT(rp.permission_id) AS total_assigned_permissions
FROM `roles` r
LEFT JOIN `role_permissions` rp ON r.id = rp.role_id
GROUP BY r.id, r.name
ORDER BY r.name ASC;

SELECT COUNT(*) AS total_role_permissions_mappings 
FROM `role_permissions`;

-- ----------------------------------------------------------------------------
-- 4. DETAILED AUDIT: PERMISSIONS ASSIGNED PER ROLE
-- ----------------------------------------------------------------------------
SELECT 
    r.name AS role_name, 
    p.name AS permission_name, 
    p.description AS permission_description
FROM `roles` r
JOIN `role_permissions` rp ON r.id = rp.role_id
JOIN `permissions` p ON rp.permission_id = p.id
ORDER BY r.name ASC, p.name ASC;

-- ----------------------------------------------------------------------------
-- 5. DUPLICATE & INTEGRITY CHECKS (Must all return 0 rows)
-- ----------------------------------------------------------------------------
-- 5a. Duplicate roles check
SELECT name, COUNT(*) AS occurrences
FROM `roles`
GROUP BY name
HAVING COUNT(*) > 1;

-- 5b. Duplicate permissions check
SELECT name, COUNT(*) AS occurrences
FROM `permissions`
GROUP BY name
HAVING COUNT(*) > 1;

-- 5c. Duplicate role-permission mappings check
SELECT role_id, permission_id, COUNT(*) AS occurrences
FROM `role_permissions`
GROUP BY role_id, permission_id
HAVING COUNT(*) > 1;

-- 5d. Referential integrity check (orphaned mappings)
SELECT rp.role_id, rp.permission_id
FROM `role_permissions` rp
LEFT JOIN `roles` r ON rp.role_id = r.id
LEFT JOIN `permissions` p ON rp.permission_id = p.id
WHERE r.id IS NULL OR p.id IS NULL;
