package com.bazaarhub.backend.order.domain.model;

/**
 * Payment processing status strictly matching MariaDB `orders.payment_status` enum.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
