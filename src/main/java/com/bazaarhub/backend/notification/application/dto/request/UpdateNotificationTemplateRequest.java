package com.bazaarhub.backend.notification.application.dto.request;

import com.bazaarhub.backend.notification.domain.model.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing notification template")
public class UpdateNotificationTemplateRequest {

    @Size(max = 255, message = "Subject must not exceed 255 characters")
    @Schema(description = "Updated notification subject", example = "Order update: {{orderNumber}}")
    private String subject;

    @Schema(description = "Updated template body with placeholders", example = "Updated notification: {{orderNumber}} status is {{status}}.")
    private String bodyTemplate;

    @Schema(description = "Updated template status (ACTIVE or INACTIVE)", example = "ACTIVE")
    private TemplateStatus status;
}
