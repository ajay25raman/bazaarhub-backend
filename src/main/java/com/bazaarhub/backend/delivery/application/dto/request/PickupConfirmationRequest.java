package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to confirm parcel pickup from seller store")
public class PickupConfirmationRequest {

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Pickup notes, e.g. packaging condition", example = "Parcel in good condition, double sealed")
    private String notes;

    @Schema(description = "Optional Cloudinary URL of pickup parcel condition photo", example = "https://res.cloudinary.com/demo/image/upload/bazaarhub/delivery/proofs/pickup_123.jpg")
    private String pickupPhotoUrl;
}
