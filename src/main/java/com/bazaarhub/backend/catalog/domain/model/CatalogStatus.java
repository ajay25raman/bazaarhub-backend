package com.bazaarhub.backend.catalog.domain.model;

/**
 * Lifecycle status for catalog taxonomy entities (Categories and Brands).
 * Strictly mirrors the MariaDB schema enum: enum('ACTIVE', 'INACTIVE').
 */
public enum CatalogStatus {
    ACTIVE,
    INACTIVE
}
