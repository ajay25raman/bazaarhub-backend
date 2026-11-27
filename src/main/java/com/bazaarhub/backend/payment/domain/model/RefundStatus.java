package com.bazaarhub.backend.payment.domain.model;

/**
 * Refund lifecycle status strictly matching MariaDB `refunds.status` enum definition.
 */
public enum RefundStatus {
    REQUESTED,
    APPROVED,
    PROCESSING,
    COMPLETED,
    REJECTED,
    FAILED,
    CANCELLED
}
