package com.bazaarhub.backend.shipping.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Value object representing a computed shipping charge quotation breakdown.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateQuote {

    private BigDecimal baseCharge;

    private BigDecimal distanceKm;

    private BigDecimal distanceCharge;

    private BigDecimal weightKg;

    private BigDecimal weightCharge;

    private boolean freeShippingApplied;

    private BigDecimal freeShippingThreshold;

    private BigDecimal totalShippingCharge;

    @Builder.Default
    private String currency = "INR";

    private String calculationMethod;
}
