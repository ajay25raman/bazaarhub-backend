package com.bazaarhub.backend.notification.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException(Long id) {
        super("Notification with ID " + id + " was not found");
    }

    public NotificationNotFoundException(String message) {
        super(message);
    }
}
