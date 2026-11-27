package com.bazaarhub.backend.delivery.application.dto.request;

import com.bazaarhub.backend.delivery.domain.model.DeliveryFailureReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to mark a delivery attempt as failed")
public class FailedDeliveryRequest {

    @NotNull(message = "Failure reason is required")
    @Schema(description = "Standard reason for delivery failure", example = "CUSTOMER_UNAVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private DeliveryFailureReason failureReason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Details about the failure attempt", example = "Attempted call 3 times, door locked")
    private String notes;

    @Schema(description = "Optional Cloudinary URL of failure evidence photo (e.g. locked gate)", example = "https://res.cloudinary.com/demo/image/upload/bazaarhub/delivery/failures/gate_locked.jpg")
    private String evidencePhotoUrl;
}
