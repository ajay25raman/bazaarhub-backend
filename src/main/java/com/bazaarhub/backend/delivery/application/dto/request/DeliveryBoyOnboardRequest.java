package com.bazaarhub.backend.delivery.application.dto.request;

import com.bazaarhub.backend.delivery.domain.model.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request payload for onboarding as a delivery partner")
public class DeliveryBoyOnboardRequest {

    @NotNull(message = "Vehicle type is required")
    @Schema(description = "Vehicle type", example = "BIKE", requiredMode = Schema.RequiredMode.REQUIRED)
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle number is required")
    @Size(max = 30, message = "Vehicle number must not exceed 30 characters")
    @Schema(description = "Registered vehicle plate number", example = "MH-12-AB-1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleNumber;
}
