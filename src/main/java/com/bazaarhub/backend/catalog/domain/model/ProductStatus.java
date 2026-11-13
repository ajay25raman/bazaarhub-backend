package com.bazaarhub.backend.catalog.domain.model;

/**
 * Product lifecycle status.
 * Strictly mirrors the MariaDB schema enum: enum('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED').
 */
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
