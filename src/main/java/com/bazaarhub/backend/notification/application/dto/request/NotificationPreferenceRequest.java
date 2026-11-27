package com.bazaarhub.backend.notification.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer notification channel preference settings")
public class NotificationPreferenceRequest {

    @Builder.Default
    @Schema(description = "Enable in-app notifications", example = "true")
    private boolean inAppEnabled = true;

    @Builder.Default
    @Schema(description = "Enable push notifications", example = "true")
    private boolean pushEnabled = true;

    @Builder.Default
    @Schema(description = "Enable transactional email notifications", example = "true")
    private boolean emailEnabled = true;

    @Builder.Default
    @Schema(description = "Enable SMS alerts", example = "false")
    private boolean smsEnabled = false;
}
