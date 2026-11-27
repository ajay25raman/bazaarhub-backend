package com.bazaarhub.backend.notification.infrastructure.provider;

import com.bazaarhub.backend.notification.domain.model.NotificationChannel;

/**
 * Contract for transactional email dispatchers.
 */
public interface EmailNotificationProvider extends NotificationChannelProvider {

    @Override
    default NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    boolean sendEmail(String recipientEmail, String subject, String bodyContent);
}
