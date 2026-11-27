package com.bazaarhub.backend.shipping.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Value object modeling package dimensions, weight, and handling instructions.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageDetails {

    @Builder.Default
    private BigDecimal weightKg = BigDecimal.valueOf(0.5);

    private BigDecimal lengthCm;

    private BigDecimal widthCm;

    private BigDecimal heightCm;

    @Builder.Default
    private Boolean fragile = false;

    @Builder.Default
    private Integer packageQuantity = 1;

    private String description;

    private String packagingNotes;

    private String conditionPhotoUrl;
}
