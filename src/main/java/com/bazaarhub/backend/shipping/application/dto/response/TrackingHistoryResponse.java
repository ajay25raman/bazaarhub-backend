package com.bazaarhub.backend.shipping.application.dto.response;

import com.bazaarhub.backend.delivery.application.dto.response.DeliveryAddressInfo;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryTrackingResponse;
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
@Schema(description = "Full authenticated tracking details and movement history")
public class TrackingHistoryResponse {

    @Schema(description = "Shipment ID", example = "10")
    private Long shipmentId;

    @Schema(description = "Shipment number", example = "SHP-A7BC3D4E5F")
    private String shipmentNumber;

    @Schema(description = "Tracking number", example = "BZH-20261114-A7BC3D")
    private String trackingNumber;

    @Schema(description = "Carrier name", example = "BazaarHub Express")
    private String carrierName;

    @Schema(description = "Current shipment status", example = "OUT_FOR_DELIVERY")
    private ShipmentStatus status;

    @Schema(description = "Origin pickup address")
    private DeliveryAddressInfo pickupAddress;

    @Schema(description = "Destination delivery address")
    private DeliveryAddressInfo deliveryAddress;

    @Schema(description = "Delivery partner name if assigned", example = "Ramesh Kumar")
    private String deliveryPartnerName;

    @Schema(description = "Delivery partner contact phone (masked for customer)", example = "+91 98*****321")
    private String deliveryPartnerPhone;

    @Schema(description = "Delivery vehicle type", example = "BIKE")
    private String vehicleType;

    @Schema(description = "Timestamp when package was picked up", example = "2026-11-14T09:30:00Z")
    private Instant shippedAt;

    @Schema(description = "Estimated delivery timestamp", example = "2026-11-14T17:00:00Z")
    private Instant estimatedDeliveryAt;

    @Schema(description = "Timestamp when package was delivered", example = "2026-11-14T15:45:00Z")
    private Instant deliveredAt;

    @Schema(description = "Delivery notes and handling logs", example = "Package picked up | Fragile")
    private String deliveryNotes;

    @Schema(description = "Lifecycle milestones")
    private List<TrackingMilestone> milestones;

    @Schema(description = "GPS coordinate breadcrumbs during transit")
    private List<DeliveryTrackingResponse> breadcrumbs;
}
