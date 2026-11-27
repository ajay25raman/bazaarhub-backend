package com.bazaarhub.backend.notification.application.dto.response;

import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
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
@Schema(description = "Notification message payload")
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "101")
    private Long id;

    @Schema(description = "Recipient user ID", example = "5")
    private Long userId;

    @Schema(description = "Notification title", example = "Order Confirmed")
    private String title;

    @Schema(description = "Notification body message", example = "Your order ORD-20261115-ABC has been confirmed.")
    private String message;

    @Schema(description = "Notification event type", example = "ORDER_CONFIRMED")
    private String notificationType;

    @Schema(description = "Channel (IN_APP, PUSH, EMAIL, SMS)", example = "IN_APP")
    private NotificationChannel channel;

    @Schema(description = "Reference entity type", example = "ORDER")
    private String referenceType;

    @Schema(description = "Reference entity ID", example = "55")
    private Long referenceId;

    @Schema(description = "Delivery status (PENDING, SENT, DELIVERED, FAILED, READ)", example = "DELIVERED")
    private DeliveryStatus deliveryStatus;

    @Schema(description = "Whether the notification has been read", example = "false")
    private boolean read;

    @Schema(description = "Timestamp when notification was read", example = "2026-11-15T06:00:00Z")
    private Instant readAt;

    @Schema(description = "Timestamp when notification was dispatched", example = "2026-11-15T05:50:00Z")
    private Instant sentAt;

    @Schema(description = "Timestamp of creation", example = "2026-11-15T05:49:00Z")
    private Instant createdAt;
}
