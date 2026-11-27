package com.bazaarhub.backend.support.application.mapper;

import com.bazaarhub.backend.support.application.dto.response.SupportMessageResponse;
import com.bazaarhub.backend.support.infrastructure.persistence.entity.SupportMessageEntity;
import org.springframework.stereotype.Component;

@Component
public class SupportMessageMapper {

    public SupportMessageResponse toResponse(SupportMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        return SupportMessageResponse.builder()
                .id(entity.getId())
                .ticketId(entity.getTicket() != null ? entity.getTicket().getId() : null)
                .senderId(entity.getSender() != null ? entity.getSender().getId() : null)
                .senderName(entity.getSender() != null ? entity.getSender().getFullName() : null)
                .senderEmail(entity.getSender() != null ? entity.getSender().getEmail() : null)
                .message(entity.getMessage())
                .attachmentUrl(entity.getAttachmentUrl())
                .isInternal(entity.isInternal())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
