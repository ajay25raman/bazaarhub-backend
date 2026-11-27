package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a shipment dispatch record for a seller sub-order")
public class CreateShipmentRequest {

    @NotNull(message = "Seller Order ID is required")
    @Schema(description = "Seller order ID", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sellerOrderId;

    @Size(max = 150, message = "Carrier name must not exceed 150 characters")
    @Schema(description = "Fulfillment carrier or fleet", example = "BazaarHub Fleet")
    private String carrierName;

    @Size(max = 150, message = "Tracking number must not exceed 150 characters")
    @Schema(description = "Optional external carrier tracking number", example = "BZH-TRK-98765")
    private String trackingNumber;

    @Schema(description = "Estimated delivery target timestamp")
    private Instant estimatedDeliveryAt;

    @Size(max = 500, message = "Delivery notes must not exceed 500 characters")
    @Schema(description = "Operational dispatch notes", example = "Fragile electronics parcel")
    private String deliveryNotes;
}
