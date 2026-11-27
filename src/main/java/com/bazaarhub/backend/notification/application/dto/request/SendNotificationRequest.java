package com.bazaarhub.backend.notification.application.dto.request;

import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to trigger and dispatch a notification")
public class SendNotificationRequest {

    @NotNull(message = "Recipient user ID is required")
    @Schema(description = "Target recipient user ID", example = "10")
    private Long userId;

    @NotBlank(message = "Notification title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Schema(description = "Notification title / headline", example = "Order Confirmed")
    private String title;

    @NotBlank(message = "Notification message is required")
    @Schema(description = "Notification body text", example = "Your order ORD-20261115-ABC has been confirmed.")
    private String message;

    @NotNull(message = "Notification type is required")
    @Schema(description = "Business notification event type", example = "ORDER_CONFIRMED")
    private NotificationType notificationType;

    @Builder.Default
    @Schema(description = "Target delivery channel (IN_APP, PUSH, EMAIL, SMS)", example = "IN_APP")
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Schema(description = "Associated reference type (e.g. ORDER, PAYMENT, SHIPMENT)", example = "ORDER")
    private String referenceType;

    @Schema(description = "Associated reference entity ID", example = "105")
    private Long referenceId;

    @Schema(description = "Unique key to enforce idempotency and prevent duplicate events", example = "ORDER-CONFIRM-105-20261115")
    private String deduplicationKey;

    @Schema(description = "Optional template key to dynamically render notification content", example = "ORDER_CONFIRMED_TEMPLATE")
    private String templateKey;

    @Schema(description = "Dynamic parameter interpolation map for template substitution")
    private Map<String, Object> templateVariables;
}
