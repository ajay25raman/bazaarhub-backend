package com.bazaarhub.backend.notification.infrastructure.provider;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;

/**
 * Contract for push notification dispatchers (e.g. Firebase Cloud Messaging).
 */
public interface PushNotificationProvider extends NotificationChannelProvider {

    @Override
    default NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    boolean sendPush(String targetToken, String title, String body, NotificationEntity notification);
}
