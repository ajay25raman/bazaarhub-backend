package com.bazaarhub.backend.shipping.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request payload to create a new internal shipment for a confirmed seller sub-order")
public class CreateInternalShipmentRequest {

    @NotNull(message = "Seller order ID is required")
    @Schema(description = "ID of the seller sub-order to ship", example = "101")
    private Long sellerOrderId;

    @Schema(description = "Carrier name for delivery", example = "BazaarHub Express")
    private String carrierName;

    @Schema(description = "Package weight in kilograms", example = "1.250")
    private BigDecimal packageWeight;

    @Schema(description = "Package length in cm", example = "25.0")
    private BigDecimal lengthCm;

    @Schema(description = "Package width in cm", example = "15.0")
    private BigDecimal widthCm;

    @Schema(description = "Package height in cm", example = "10.0")
    private BigDecimal heightCm;

    @Schema(description = "Indicates if parcel contains fragile items", example = "false")
    private Boolean isFragile;

    @Schema(description = "Special delivery or handling instructions", example = "Handle with care, call customer upon arrival")
    private String deliveryNotes;

    @Schema(description = "Estimated transit duration in days", example = "2")
    private Integer estimatedDeliveryDays;
}
