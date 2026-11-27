package com.bazaarhub.backend.notification.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class NotificationTemplateNotFoundException extends ResourceNotFoundException {

    public NotificationTemplateNotFoundException(Long id) {
        super("NotificationTemplate with ID " + id + " was not found");
    }

    public NotificationTemplateNotFoundException(String templateKey, String channel) {
        super(String.format("NotificationTemplate with key '%s' and channel '%s' was not found", templateKey, channel));
    }
}
