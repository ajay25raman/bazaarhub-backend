package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update real-time GPS location of delivery partner")
public class UpdateLocationRequest {

    @NotNull(message = "Shipment ID is required")
    @Schema(description = "Shipment ID currently being delivered", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shipmentId;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Current GPS latitude coordinate", example = "19.076090", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Current GPS longitude coordinate", example = "72.877426", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal longitude;
}
