package com.bazaarhub.backend.catalog.domain.model;

/**
 * Product administrative review & moderation approval status.
 * Strictly mirrors the MariaDB schema enum: enum('PENDING', 'APPROVED', 'REJECTED').
 */
public enum ProductApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
