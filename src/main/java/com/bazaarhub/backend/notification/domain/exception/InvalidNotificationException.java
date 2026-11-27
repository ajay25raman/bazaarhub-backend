package com.bazaarhub.backend.notification.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidNotificationException extends DomainException {

    public InvalidNotificationException(String message) {
        super(message, "INVALID_NOTIFICATION");
    }
}
