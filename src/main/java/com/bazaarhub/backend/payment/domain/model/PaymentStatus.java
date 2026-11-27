package com.bazaarhub.backend.payment.domain.model;

/**
 * Payment lifecycle status strictly matching MariaDB `payments.status` enum definition.
 */
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
