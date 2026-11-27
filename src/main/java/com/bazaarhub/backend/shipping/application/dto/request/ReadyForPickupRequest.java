package com.bazaarhub.backend.shipping.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload when seller marks parcel packaged and ready for delivery partner pickup")
public class ReadyForPickupRequest {

    @Positive(message = "Package weight must be positive")
    @Schema(description = "Actual weighed package weight in kilograms", example = "1.350")
    private BigDecimal packageWeight;

    @Positive(message = "Package length must be positive")
    @Schema(description = "Package length in cm", example = "25.0")
    private BigDecimal lengthCm;

    @Positive(message = "Package width must be positive")
    @Schema(description = "Package width in cm", example = "15.0")
    private BigDecimal widthCm;

    @Positive(message = "Package height must be positive")
    @Schema(description = "Package height in cm", example = "10.0")
    private BigDecimal heightCm;

    @Schema(description = "Fragile parcel indicator", example = "false")
    private Boolean isFragile;

    @Size(max = 500, message = "Packaging notes must not exceed 500 characters")
    @Schema(description = "Packaging notes or store pickup instructions", example = "Packaged in bubble wrap at Store Counter 2")
    private String notes;

    @Size(max = 1000, message = "Packaging photo URL must not exceed 1000 characters")
    @Schema(description = "Cloudinary URL of packaged parcel image", example = "https://res.cloudinary.com/.../parcel1.jpg")
    private String packagingPhotoUrl;
}
