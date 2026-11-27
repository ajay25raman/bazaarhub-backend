package com.bazaarhub.backend.delivery.application.dto.response;

import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.domain.model.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Delivery partner profile details")
public class DeliveryBoyResponse {

    @Schema(description = "Delivery boy profile ID", example = "5")
    private Long id;

    @Schema(description = "Associated user ID", example = "20")
    private Long userId;

    @Schema(description = "Full name", example = "Ramesh Kumar")
    private String fullName;

    @Schema(description = "Registered email", example = "ramesh.kumar@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Vehicle type", example = "BIKE")
    private VehicleType vehicleType;

    @Schema(description = "Vehicle registration number", example = "MH-12-AB-1234")
    private String vehicleNumber;

    @Schema(description = "KYC verification status", example = "VERIFIED")
    private DeliveryBoyVerificationStatus verificationStatus;

    @Schema(description = "Current operational availability status", example = "AVAILABLE")
    private DeliveryBoyAvailabilityStatus availabilityStatus;

    @Schema(description = "Timestamp when KYC was verified")
    private Instant verifiedAt;

    @Schema(description = "Profile creation timestamp")
    private Instant createdAt;
}
