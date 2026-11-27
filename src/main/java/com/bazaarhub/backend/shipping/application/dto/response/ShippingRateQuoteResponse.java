package com.bazaarhub.backend.shipping.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Calculated internal shipping charge breakdown")
public class ShippingRateQuoteResponse {

    @Schema(description = "Base shipping charge", example = "40.00")
    private BigDecimal baseCharge;

    @Schema(description = "Distance in kilometers", example = "6.5")
    private BigDecimal distanceKm;

    @Schema(description = "Distance-based charge surcharge", example = "7.50")
    private BigDecimal distanceCharge;

    @Schema(description = "Weight in kilograms", example = "1.500")
    private BigDecimal weightKg;

    @Schema(description = "Weight-based surcharge", example = "5.00")
    private BigDecimal weightCharge;

    @Schema(description = "Whether free shipping was applied", example = "false")
    private boolean freeShippingApplied;

    @Schema(description = "Order subtotal threshold required for free shipping", example = "500.00")
    private BigDecimal freeShippingThreshold;

    @Schema(description = "Final calculated total shipping amount", example = "52.50")
    private BigDecimal totalShippingCharge;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Calculation strategy description", example = "INTERNAL_DISTANCE_WEIGHT_MODEL")
    private String calculationMethod;
}
