package com.bazaarhub.backend.delivery.application.dto.response;

import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
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
@Schema(description = "Delivery assignment response for dispatch operations")
public class DeliveryAssignmentResponse {

    @Schema(description = "Assignment ID", example = "50")
    private Long id;

    @Schema(description = "Shipment ID", example = "101")
    private Long shipmentId;

    @Schema(description = "Shipment number", example = "SHP-7A8B9C0D")
    private String shipmentNumber;

    @Schema(description = "Delivery boy profile ID", example = "5")
    private Long deliveryBoyId;

    @Schema(description = "Delivery boy full name", example = "Ramesh Kumar")
    private String deliveryBoyName;

    @Schema(description = "Delivery boy phone", example = "+919876543210")
    private String deliveryBoyPhone;

    @Schema(description = "Current assignment status", example = "ASSIGNED")
    private DeliveryAssignmentStatus assignmentStatus;

    @Schema(description = "Timestamp when assigned")
    private Instant assignedAt;

    @Schema(description = "Timestamp when accepted by delivery boy")
    private Instant acceptedAt;

    @Schema(description = "Timestamp when delivery completed")
    private Instant completedAt;

    @Schema(description = "Rejection reason if rejected", example = "Bike issue")
    private String rejectionReason;

    @Schema(description = "Full shipment and order details")
    private ShipmentResponse shipmentDetails;
}
