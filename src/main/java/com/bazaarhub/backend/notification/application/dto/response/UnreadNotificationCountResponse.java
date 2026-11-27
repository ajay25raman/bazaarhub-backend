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
@Schema(description = "Unread notifications count summary")
public class UnreadNotificationCountResponse {

    @Schema(description = "User ID", example = "5")
    private Long userId;

    @Schema(description = "Count of unread notifications", example = "3")
    private long unreadCount;
}
