package com.bazaarhub.backend.delivery.application.dto.request;

import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update operational availability status")
public class UpdateAvailabilityRequest {

    @NotNull(message = "Availability status is required")
    @Schema(description = "Target availability status (AVAILABLE or OFFLINE)", example = "AVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private DeliveryBoyAvailabilityStatus availabilityStatus;
}
