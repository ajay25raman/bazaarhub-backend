package com.bazaarhub.backend.support.application.mapper;

import com.bazaarhub.backend.support.application.dto.response.SupportTicketResponse;
import com.bazaarhub.backend.support.infrastructure.persistence.entity.SupportTicketEntity;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketMapper {

    public SupportTicketResponse toResponse(SupportTicketEntity entity, long messageCount) {
        if (entity == null) {
            return null;
        }

        return SupportTicketResponse.builder()
                .id(entity.getId())
                .ticketNumber(entity.getTicketNumber())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userFullName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .orderNumber(entity.getOrder() != null ? entity.getOrder().getOrderNumber() : null)
                .category(entity.getCategory())
                .subject(entity.getSubject())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .assignedToId(entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null)
                .assignedToName(entity.getAssignedTo() != null ? entity.getAssignedTo().getFullName() : null)
                .resolvedAt(entity.getResolvedAt())
                .closedAt(entity.getClosedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .messageCount(messageCount)
                .build();
    }

    public SupportTicketResponse toResponse(SupportTicketEntity entity) {
        return toResponse(entity, 0L);
    }
}
