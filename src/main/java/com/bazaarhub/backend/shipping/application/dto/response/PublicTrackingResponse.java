package com.bazaarhub.backend.shipping.application.dto.response;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.shipping.domain.model.TrackingMilestone;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public, non-sensitive tracking summary for customer or public lookup")
public class PublicTrackingResponse {

    @Schema(description = "Public tracking number", example = "BZH-20261114-A7BC3D")
    private String trackingNumber;

    @Schema(description = "Shipment reference number", example = "SHP-A7BC3D4E5F")
    private String shipmentNumber;

    @Schema(description = "Carrier name", example = "BazaarHub Express")
    private String carrierName;

    @Schema(description = "Current shipment status", example = "OUT_FOR_DELIVERY")
    private ShipmentStatus status;

    @Schema(description = "Origin dispatch city", example = "Bengaluru")
    private String originCity;

    @Schema(description = "Destination city", example = "Bengaluru")
    private String destinationCity;

    @Schema(description = "Destination state", example = "Karnataka")
    private String destinationState;

    @Schema(description = "Timestamp when package was picked up", example = "2026-11-14T09:30:00Z")
    private Instant shippedAt;

    @Schema(description = "Estimated delivery timestamp", example = "2026-11-14T17:00:00Z")
    private Instant estimatedDeliveryAt;

    @Schema(description = "Timestamp when package was delivered", example = "2026-11-14T15:45:00Z")
    private Instant deliveredAt;

    @Schema(description = "List of tracking lifecycle milestones")
    private List<TrackingMilestone> milestones;
}
