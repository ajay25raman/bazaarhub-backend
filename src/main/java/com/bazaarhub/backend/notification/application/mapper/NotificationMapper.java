package com.bazaarhub.backend.notification.application.mapper;

import com.bazaarhub.backend.notification.application.dto.response.NotificationResponse;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .title(entity.getTitle())
                .message(entity.getMessage())
                .notificationType(entity.getNotificationType())
                .channel(entity.getChannel())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .deliveryStatus(entity.getDeliveryStatus())
                .read(entity.isRead())
                .readAt(entity.getReadAt())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
