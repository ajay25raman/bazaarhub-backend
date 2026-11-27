package com.bazaarhub.backend.notification.application.mapper;

import com.bazaarhub.backend.notification.application.dto.response.NotificationTemplateResponse;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateMapper {

    public NotificationTemplateResponse toResponse(NotificationTemplateEntity entity) {
        if (entity == null) {
            return null;
        }

        return NotificationTemplateResponse.builder()
                .id(entity.getId())
                .templateKey(entity.getTemplateKey())
                .channel(entity.getChannel())
                .subject(entity.getSubject())
                .bodyTemplate(entity.getBodyTemplate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
