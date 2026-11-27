package com.bazaarhub.backend.notification.infrastructure.provider;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;

/**
 * Common SPI contract for channel-specific notification dispatchers.
 */
public interface NotificationChannelProvider {

    NotificationChannel getChannel();

    boolean send(NotificationEntity notification, UserEntity recipient);
}
