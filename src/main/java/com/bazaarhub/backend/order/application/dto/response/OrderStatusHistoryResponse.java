package com.bazaarhub.backend.order.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Order status transition audit history entry")
public class OrderStatusHistoryResponse {

    @Schema(description = "History entry ID", example = "1")
    private Long id;

    @Schema(description = "Associated order ID", example = "10")
    private Long orderId;

    @Schema(description = "Associated seller order ID if applicable", example = "25")
    private Long sellerOrderId;

    @Schema(description = "Previous status", example = "PENDING")
    private String previousStatus;

    @Schema(description = "New status transitioned to", example = "CONFIRMED")
    private String newStatus;

    @Schema(description = "User ID who triggered the change", example = "3")
    private Long changedByUserId;

    @Schema(description = "Name of the user who triggered the change", example = "Ajay Kumar")
    private String changedByUserName;

    @Schema(description = "Remarks or operational notes", example = "Payment received, order confirmed.")
    private String remarks;

    @Schema(description = "Timestamp when transition was recorded")
    private Instant createdAt;
}
