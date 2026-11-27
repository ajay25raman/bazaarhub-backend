package com.bazaarhub.backend.notification.domain.model;

/**
 * Delivery status for notifications.
 * Strictly maps to MariaDB enum: 'PENDING', 'SENT', 'DELIVERED', 'FAILED', 'READ'.
 */
public enum DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    READ
}
