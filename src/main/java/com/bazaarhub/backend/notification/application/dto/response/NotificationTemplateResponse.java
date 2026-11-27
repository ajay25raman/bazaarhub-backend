package com.bazaarhub.backend.notification.application.dto.response;

import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification template details")
public class NotificationTemplateResponse {

    @Schema(description = "Template ID", example = "1")
    private Long id;

    @Schema(description = "Unique template key", example = "ORDER_CONFIRMED")
    private String templateKey;

    @Schema(description = "Channel (IN_APP, PUSH, EMAIL, SMS)", example = "PUSH")
    private NotificationChannel channel;

    @Schema(description = "Notification subject", example = "Your order is confirmed!")
    private String subject;

    @Schema(description = "Body template with placeholders", example = "Hello {{customerName}}, your order {{orderNumber}} is confirmed.")
    private String bodyTemplate;

    @Schema(description = "Status (ACTIVE or INACTIVE)", example = "ACTIVE")
    private TemplateStatus status;

    @Schema(description = "Creation timestamp", example = "2026-11-15T00:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-15T00:00:00Z")
    private Instant updatedAt;
}
