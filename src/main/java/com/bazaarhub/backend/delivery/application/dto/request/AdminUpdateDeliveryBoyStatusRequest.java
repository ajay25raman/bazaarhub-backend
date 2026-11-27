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
@Schema(description = "Admin request to adjust delivery boy availability/account status")
public class AdminUpdateDeliveryBoyStatusRequest {

    @NotNull(message = "Availability status is required")
    @Schema(description = "Target status (AVAILABLE, OFFLINE, SUSPENDED)", example = "SUSPENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    private DeliveryBoyAvailabilityStatus availabilityStatus;

    @Schema(description = "Reason for suspension or status modification", example = "Policy violation reported by seller")
    private String reason;
}
