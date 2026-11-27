package com.bazaarhub.backend.delivery.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Real-time delivery location track point")
public class DeliveryTrackingResponse {

    @Schema(description = "Tracking record ID", example = "1001")
    private Long id;

    @Schema(description = "Shipment ID", example = "101")
    private Long shipmentId;

    @Schema(description = "Delivery boy profile ID", example = "5")
    private Long deliveryBoyId;

    @Schema(description = "Latitude", example = "19.076090")
    private BigDecimal latitude;

    @Schema(description = "Longitude", example = "72.877426")
    private BigDecimal longitude;

    @Schema(description = "Recorded timestamp")
    private Instant recordedAt;
}
