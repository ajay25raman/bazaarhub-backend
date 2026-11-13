-- ============================================================================
-- BazaarHub Multi-Vendor Marketplace - Phase 1 RBAC Seed Script
-- ============================================================================
-- Target Database : MariaDB 11.8.9 (u205680228_bazaarhub_db)
-- Target Tables   : roles, permissions, role_permissions
-- Safety Level    : Non-destructive, Idempotent, Duplicate-Resistant
-- Strategy        : INSERT IGNORE with dynamic role/permission key joins
-- Constraints     : No table schema modifications; no user_roles changes
-- ============================================================================

START TRANSACTION;

-- ============================================================================
-- 1. SEED PLATFORM ROLES
-- ============================================================================
INSERT IGNORE INTO `roles` (`name`, `description`, `created_at`, `updated_at`) VALUES
  ('ROLE_CUSTOMER', 'Standard retail customer purchasing goods on BazaarHub marketplace', NOW(3), NOW(3)),
  ('ROLE_SELLER', 'Merchant selling products and managing store operations on BazaarHub', NOW(3), NOW(3)),
  ('ROLE_DELIVERY', 'Logistics and delivery partner fulfilling orders on BazaarHub', NOW(3), NOW(3)),
  ('ROLE_ADMIN', 'System administrator with full platform oversight and management capabilities', NOW(3), NOW(3));

-- ============================================================================
-- 2. SEED PLATFORM PERMISSIONS (68 Granular Permissions across 12 Categories)
-- ============================================================================
INSERT IGNORE INTO `permissions` (`name`, `description`, `created_at`, `updated_at`) VALUES
  ('CUSTOMER_PROFILE_READ', 'View customer personal profile, contact information, and address book', NOW(3), NOW(3)),
  ('CUSTOMER_PROFILE_UPDATE', 'Modify customer personal profile, contact information, and address book', NOW(3), NOW(3)),
  ('CUSTOMER_ACCOUNT_DELETE', 'Request closure or deletion of customer account', NOW(3), NOW(3)),
  ('CART_READ', 'View items and calculated totals in the customer active shopping cart', NOW(3), NOW(3)),
  ('CART_UPDATE', 'Add, modify quantities, or remove items from the customer active shopping cart', NOW(3), NOW(3)),
  ('WISHLIST_READ', 'View saved products in customer personal wishlist', NOW(3), NOW(3)),
  ('WISHLIST_UPDATE', 'Add or remove items from customer personal wishlist', NOW(3), NOW(3)),
  ('SELLER_PROFILE_READ', 'View seller personal and business registration details', NOW(3), NOW(3)),
  ('SELLER_PROFILE_UPDATE', 'Modify seller contact and business profile information', NOW(3), NOW(3)),
  ('SELLER_ONBOARDING_SUBMIT', 'Submit business verification documents and KYC details for seller onboarding', NOW(3), NOW(3)),
  ('STORE_READ', 'View store profile, branding, banner, and operating configuration', NOW(3), NOW(3)),
  ('STORE_UPDATE', 'Update store details, policies, store banner, logo, and operating hours', NOW(3), NOW(3)),
  ('PRODUCT_READ', 'View active product catalog, product details, specifications, and pricing', NOW(3), NOW(3)),
  ('PRODUCT_CREATE', 'Create new product listings for the authenticated seller store', NOW(3), NOW(3)),
  ('PRODUCT_UPDATE', 'Update product listings, descriptions, media, and prices for seller own products', NOW(3), NOW(3)),
  ('PRODUCT_DELETE', 'Delete or archive product listings for seller own store', NOW(3), NOW(3)),
  ('CATEGORY_READ', 'View catalog categories, category hierarchy, and navigation trees', NOW(3), NOW(3)),
  ('BRAND_READ', 'View active brands associated with catalog products', NOW(3), NOW(3)),
  ('PRODUCT_MODERATE', 'Review, approve, reject, ban, or delist products across the platform', NOW(3), NOW(3)),
  ('CATEGORY_MANAGE', 'Create, edit, reorder, or delete product categories', NOW(3), NOW(3)),
  ('BRAND_MANAGE', 'Create, update, or remove brands in the platform catalog', NOW(3), NOW(3)),
  ('INVENTORY_READ', 'View current stock counts, reservations, and SKU thresholds for own store', NOW(3), NOW(3)),
  ('INVENTORY_UPDATE', 'Adjust stock levels, restock SKUs, and manage warehouse availability for own products', NOW(3), NOW(3)),
  ('INVENTORY_AUDIT', 'Inspect platform-wide stock logs, inventory audit trails, and discrepancy records', NOW(3), NOW(3)),
  ('ORDER_CREATE', 'Place new customer orders from active cart contents', NOW(3), NOW(3)),
  ('ORDER_READ', 'View customer own order history, tracking details, and receipts', NOW(3), NOW(3)),
  ('ORDER_CANCEL', 'Cancel customer own pending or unfulfilled orders within cancellation window', NOW(3), NOW(3)),
  ('SELLER_ORDER_READ', 'View customer orders containing items from authenticated seller store', NOW(3), NOW(3)),
  ('SELLER_ORDER_UPDATE', 'Update order fulfillment lifecycle (confirm, prepare, pack) for seller order items', NOW(3), NOW(3)),
  ('DELIVERY_ORDER_READ', 'View essential order and delivery manifest details for assigned deliveries', NOW(3), NOW(3)),
  ('ORDER_MANAGE', 'Platform-wide order management: inspect any order, force status transitions, handle disputes', NOW(3), NOW(3)),
  ('DELIVERY_PROFILE_READ', 'View delivery partner profile, vehicle information, and verification status', NOW(3), NOW(3)),
  ('DELIVERY_PROFILE_UPDATE', 'Update delivery partner availability, shift status, and contact phone', NOW(3), NOW(3)),
  ('DELIVERY_ASSIGNMENT_READ', 'View delivery assignments, packages ready for pickup, and drop-off destinations', NOW(3), NOW(3)),
  ('DELIVERY_STATUS_UPDATE', 'Transition delivery milestones (pickup confirmed, transit, delivered, delivery failed)', NOW(3), NOW(3)),
  ('DELIVERY_LOCATION_UPDATE', 'Transmit live GPS/location telemetry for package tracking', NOW(3), NOW(3)),
  ('DELIVERY_MANAGE', 'Oversee platform logistics: assign deliveries, monitor transit times, reassign runs', NOW(3), NOW(3)),
  ('PAYMENT_CREATE', 'Initiate checkout payments and process gateway transactions', NOW(3), NOW(3)),
  ('PAYMENT_READ', 'View customer payment receipts, transaction IDs, and invoice records', NOW(3), NOW(3)),
  ('REFUND_REQUEST', 'Initiate a refund or return request for an eligible completed order', NOW(3), NOW(3)),
  ('SELLER_PAYMENT_READ', 'View settlement breakdowns and gross order transaction amounts for seller sales', NOW(3), NOW(3)),
  ('PAYMENT_MANAGE', 'View all gateway transactions, ledger entries, chargebacks, and payment reconciliation', NOW(3), NOW(3)),
  ('REFUND_MANAGE', 'Review, approve, dispute, or disburse customer refunds and return authorizations', NOW(3), NOW(3)),
  ('COMMISSION_READ', 'View applicable platform commission rates, fee deductions, and net revenue calculations', NOW(3), NOW(3)),
  ('PAYOUT_READ', 'View payout ledgers, historical transfers, pending disbursements, and balance statements', NOW(3), NOW(3)),
  ('PAYOUT_REQUEST', 'Request payout disbursement to registered bank account', NOW(3), NOW(3)),
  ('COMMISSION_MANAGE', 'Configure and adjust marketplace commission tiers, seller fees, and category rates', NOW(3), NOW(3)),
  ('PAYOUT_MANAGE', 'Authorize, process, hold, or execute payout batches to sellers and delivery partners', NOW(3), NOW(3)),
  ('REVIEW_CREATE', 'Submit product ratings, reviews, and verified buyer feedback', NOW(3), NOW(3)),
  ('REVIEW_READ', 'View public customer reviews and merchant ratings across products and stores', NOW(3), NOW(3)),
  ('REVIEW_UPDATE', 'Edit own previously submitted reviews within allowable edit window', NOW(3), NOW(3)),
  ('REVIEW_DELETE', 'Remove own previously submitted reviews', NOW(3), NOW(3)),
  ('SELLER_REVIEW_REPLY', 'Post merchant responses to customer reviews left on seller products', NOW(3), NOW(3)),
  ('REVIEW_MODERATE', 'Moderate, hide, restore, or purge abusive or fraudulent reviews', NOW(3), NOW(3)),
  ('SUPPORT_TICKET_CREATE', 'Submit support tickets, incident inquiries, or dispute requests', NOW(3), NOW(3)),
  ('SUPPORT_TICKET_READ', 'View communication history and status updates for own submitted support tickets', NOW(3), NOW(3)),
  ('SUPPORT_TICKET_REPLY', 'Send messages and attachments on own active support tickets', NOW(3), NOW(3)),
  ('SUPPORT_TICKET_MANAGE', 'View, assign, categorize, escalate, resolve, and close all platform support tickets', NOW(3), NOW(3)),
  ('USER_MANAGE', 'View all user accounts, update statuses, and manage user lifecycle', NOW(3), NOW(3)),
  ('ROLE_MANAGE', 'Create, modify, assign, or revoke roles and permissions across system users', NOW(3), NOW(3)),
  ('SELLER_MANAGE', 'Approve new seller registrations, verify merchant credentials, audit or suspend store operations', NOW(3), NOW(3)),
  ('DELIVERY_PARTNER_MANAGE', 'Onboard, verify, audit, or suspend delivery personnel and partner fleets', NOW(3), NOW(3)),
  ('PLATFORM_SETTINGS_MANAGE', 'Configure marketplace-wide settings, operational parameters, and feature toggles', NOW(3), NOW(3)),
  ('AUDIT_LOG_READ', 'Search, filter, and inspect immutable system security events and administrative audit logs', NOW(3), NOW(3)),
  ('SELLER_ANALYTICS_READ', 'View sales figures, inventory turnover, store traffic, and merchant performance metrics', NOW(3), NOW(3)),
  ('DELIVERY_ANALYTICS_READ', 'View delivery success rates, completion times, and courier performance metrics', NOW(3), NOW(3)),
  ('PLATFORM_ANALYTICS_READ', 'Access executive dashboards, gross merchandise value (GMV), platform growth, and macro metrics', NOW(3), NOW(3)),
  ('REPORT_EXPORT', 'Generate and download system-wide financial, tax, compliance, and operational reports', NOW(3), NOW(3));

-- ============================================================================
-- 3. SEED ROLE-PERMISSION MAPPINGS (Dynamic ID Resolution)
-- ============================================================================
-- ----------------------------------------------------------------------------
-- Role: ROLE_CUSTOMER (23 permissions)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.name IN (
    'CUSTOMER_PROFILE_READ',
    'CUSTOMER_PROFILE_UPDATE',
    'CUSTOMER_ACCOUNT_DELETE',
    'CART_READ',
    'CART_UPDATE',
    'WISHLIST_READ',
    'WISHLIST_UPDATE',
    'PRODUCT_READ',
    'CATEGORY_READ',
    'BRAND_READ',
    'ORDER_CREATE',
    'ORDER_READ',
    'ORDER_CANCEL',
    'PAYMENT_CREATE',
    'PAYMENT_READ',
    'REFUND_REQUEST',
    'REVIEW_CREATE',
    'REVIEW_READ',
    'REVIEW_UPDATE',
    'REVIEW_DELETE',
    'SUPPORT_TICKET_CREATE',
    'SUPPORT_TICKET_READ',
    'SUPPORT_TICKET_REPLY'
)
WHERE r.name = 'ROLE_CUSTOMER';

-- ----------------------------------------------------------------------------
-- Role: ROLE_SELLER (25 permissions)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.name IN (
    'SELLER_PROFILE_READ',
    'SELLER_PROFILE_UPDATE',
    'SELLER_ONBOARDING_SUBMIT',
    'STORE_READ',
    'STORE_UPDATE',
    'PRODUCT_READ',
    'PRODUCT_CREATE',
    'PRODUCT_UPDATE',
    'PRODUCT_DELETE',
    'CATEGORY_READ',
    'BRAND_READ',
    'INVENTORY_READ',
    'INVENTORY_UPDATE',
    'SELLER_ORDER_READ',
    'SELLER_ORDER_UPDATE',
    'SELLER_PAYMENT_READ',
    'COMMISSION_READ',
    'PAYOUT_READ',
    'PAYOUT_REQUEST',
    'REVIEW_READ',
    'SELLER_REVIEW_REPLY',
    'SUPPORT_TICKET_CREATE',
    'SUPPORT_TICKET_READ',
    'SUPPORT_TICKET_REPLY',
    'SELLER_ANALYTICS_READ'
)
WHERE r.name = 'ROLE_SELLER';

-- ----------------------------------------------------------------------------
-- Role: ROLE_DELIVERY (11 permissions)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.name IN (
    'DELIVERY_PROFILE_READ',
    'DELIVERY_PROFILE_UPDATE',
    'DELIVERY_ASSIGNMENT_READ',
    'DELIVERY_STATUS_UPDATE',
    'DELIVERY_LOCATION_UPDATE',
    'DELIVERY_ORDER_READ',
    'PAYOUT_READ',
    'SUPPORT_TICKET_CREATE',
    'SUPPORT_TICKET_READ',
    'SUPPORT_TICKET_REPLY',
    'DELIVERY_ANALYTICS_READ'
)
WHERE r.name = 'ROLE_DELIVERY';

-- ----------------------------------------------------------------------------
-- Role: ROLE_ADMIN (34 permissions)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.name IN (
    'CUSTOMER_PROFILE_READ',
    'SELLER_PROFILE_READ',
    'STORE_READ',
    'PRODUCT_READ',
    'PRODUCT_MODERATE',
    'CATEGORY_READ',
    'CATEGORY_MANAGE',
    'BRAND_READ',
    'BRAND_MANAGE',
    'INVENTORY_READ',
    'INVENTORY_AUDIT',
    'ORDER_READ',
    'ORDER_MANAGE',
    'DELIVERY_ASSIGNMENT_READ',
    'DELIVERY_MANAGE',
    'PAYMENT_READ',
    'PAYMENT_MANAGE',
    'REFUND_MANAGE',
    'COMMISSION_READ',
    'COMMISSION_MANAGE',
    'PAYOUT_READ',
    'PAYOUT_MANAGE',
    'REVIEW_READ',
    'REVIEW_MODERATE',
    'SUPPORT_TICKET_READ',
    'SUPPORT_TICKET_MANAGE',
    'USER_MANAGE',
    'ROLE_MANAGE',
    'SELLER_MANAGE',
    'DELIVERY_PARTNER_MANAGE',
    'PLATFORM_SETTINGS_MANAGE',
    'AUDIT_LOG_READ',
    'PLATFORM_ANALYTICS_READ',
    'REPORT_EXPORT'
)
WHERE r.name = 'ROLE_ADMIN';

-- ============================================================================
-- COMMIT TRANSACTION
-- ============================================================================
COMMIT;
