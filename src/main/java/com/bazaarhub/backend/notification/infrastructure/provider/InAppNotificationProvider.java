package com.bazaarhub.backend.notification.infrastructure.provider;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * In-app notification provider.
 * Delivers notifications directly to the user's database inbox.
 */
@Component
public class InAppNotificationProvider implements NotificationChannelProvider {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationProvider.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public boolean send(NotificationEntity notification, UserEntity recipient) {
        log.info("[IN-APP NOTIFICATION] Inbox message delivered for user ID: {} | Title: '{}'",
                recipient != null ? recipient.getId() : null, notification.getTitle());
        notification.setDeliveryStatus(DeliveryStatus.DELIVERED);
        notification.setSentAt(Instant.now());
        return true;
    }
}
