package com.bazaarhub.backend.order.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update order or seller sub-order status")
public class UpdateOrderStatusRequest {

    @NotBlank(message = "New status is required")
    @Schema(description = "Target status (e.g., CONFIRMED, PROCESSING, READY_FOR_PICKUP, SHIPPED, DELIVERED, CANCELLED)", example = "CONFIRMED")
    private String status;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    @Schema(description = "Remarks or audit note explaining the status change", example = "Order processed and packed for shipment.")
    private String remarks;
}
