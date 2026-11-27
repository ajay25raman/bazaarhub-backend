package com.bazaarhub.backend.notification.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedNotificationAccessException extends DomainException {

    public UnauthorizedNotificationAccessException(Long userId, Long notificationId) {
        super(String.format("User %d is not authorized to access notification %d", userId, notificationId), "UNAUTHORIZED_NOTIFICATION_ACCESS");
    }

    public UnauthorizedNotificationAccessException(String message) {
        super(message, "UNAUTHORIZED_NOTIFICATION_ACCESS");
    }
}
