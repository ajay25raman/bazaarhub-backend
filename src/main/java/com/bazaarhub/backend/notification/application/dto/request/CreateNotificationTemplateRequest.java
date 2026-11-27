package com.bazaarhub.backend.notification.application.dto.request;

import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new notification template")
public class CreateNotificationTemplateRequest {

    @NotBlank(message = "Template key is required")
    @Size(max = 100, message = "Template key must not exceed 100 characters")
    @Schema(description = "Unique template identifier key", example = "ORDER_CONFIRMED")
    private String templateKey;

    @NotNull(message = "Delivery channel is required")
    @Schema(description = "Target channel (IN_APP, PUSH, EMAIL, SMS)", example = "PUSH")
    private NotificationChannel channel;

    @Size(max = 255, message = "Subject must not exceed 255 characters")
    @Schema(description = "Optional notification subject", example = "Your order has been confirmed!")
    private String subject;

    @NotBlank(message = "Body template is required")
    @Schema(description = "Body template containing variable placeholders (e.g. {{orderNumber}})",
            example = "Hello {{customerName}}, your order {{orderNumber}} for ₹{{totalAmount}} is confirmed.")
    private String bodyTemplate;

    @Builder.Default
    @Schema(description = "Template status (ACTIVE or INACTIVE)", example = "ACTIVE")
    private TemplateStatus status = TemplateStatus.ACTIVE;
}
