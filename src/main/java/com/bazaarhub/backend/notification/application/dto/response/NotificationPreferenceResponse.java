package com.bazaarhub.backend.notification.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification preference response")
public class NotificationPreferenceResponse {

    @Schema(description = "User ID", example = "5")
    private Long userId;

    @Schema(description = "Whether in-app inbox notifications are enabled", example = "true")
    private boolean inAppEnabled;

    @Schema(description = "Whether push notifications are enabled", example = "true")
    private boolean pushEnabled;

    @Schema(description = "Whether email notifications are enabled", example = "true")
    private boolean emailEnabled;

    @Schema(description = "Whether SMS notifications are enabled", example = "false")
    private boolean smsEnabled;
}
