package com.bazaarhub.backend.notification.domain.model;

/**
 * Notification template lifecycle status.
 * Strictly maps to MariaDB enum: 'ACTIVE', 'INACTIVE'.
 */
public enum TemplateStatus {
    ACTIVE,
    INACTIVE
}
