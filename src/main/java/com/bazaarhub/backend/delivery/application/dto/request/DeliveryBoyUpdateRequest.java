package com.bazaarhub.backend.delivery.application.dto.request;

import com.bazaarhub.backend.delivery.domain.model.VehicleType;
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
@Schema(description = "Request payload for updating delivery partner profile details")
public class DeliveryBoyUpdateRequest {

    @Schema(description = "Updated vehicle type", example = "SCOOTER")
    private VehicleType vehicleType;

    @Size(max = 30, message = "Vehicle number must not exceed 30 characters")
    @Schema(description = "Updated vehicle plate number", example = "DL-01-XY-5678")
    private String vehicleNumber;
}
