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
@Schema(description = "Package and parcel specifications")
public class PackageInfoResponse {

    @Schema(description = "Estimated or measured weight in kg", example = "1.250")
    private BigDecimal weightKg;

    @Schema(description = "Length in cm", example = "25.0")
    private BigDecimal lengthCm;

    @Schema(description = "Width in cm", example = "15.0")
    private BigDecimal widthCm;

    @Schema(description = "Height in cm", example = "10.0")
    private BigDecimal heightCm;

    @Schema(description = "Fragile indicator", example = "false")
    private Boolean fragile;

    @Schema(description = "Package count", example = "1")
    private Integer packageQuantity;

    @Schema(description = "Special instructions or notes", example = "Keep away from moisture")
    private String notes;

    @Schema(description = "URL of package condition proof photo", example = "https://res.cloudinary.com/.../parcel1.jpg")
    private String conditionPhotoUrl;
}
