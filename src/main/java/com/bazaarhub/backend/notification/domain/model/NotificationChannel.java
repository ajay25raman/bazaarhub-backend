package com.bazaarhub.backend.notification.domain.model;

/**
 * Communication channels supported by BazaarHub.
 * Strictly maps to MariaDB enum: 'IN_APP', 'PUSH', 'EMAIL', 'SMS'.
 */
public enum NotificationChannel {
    IN_APP,
    PUSH,
    EMAIL,
    SMS
}
