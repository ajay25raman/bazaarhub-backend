package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to assign a delivery boy to a shipment")
public class AssignDeliveryBoyRequest {

    @NotNull(message = "Shipment ID is required")
    @Schema(description = "Target shipment ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shipmentId;

    @NotNull(message = "Delivery boy ID is required")
    @Schema(description = "Target delivery boy ID", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deliveryBoyId;
}
