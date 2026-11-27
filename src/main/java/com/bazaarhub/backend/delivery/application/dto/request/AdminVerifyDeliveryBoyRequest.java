package com.bazaarhub.backend.delivery.application.dto.request;

import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
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
@Schema(description = "Admin request to verify or reject delivery boy KYC")
public class AdminVerifyDeliveryBoyRequest {

    @NotNull(message = "Verification status is required")
    @Schema(description = "Target verification status (VERIFIED or REJECTED)", example = "VERIFIED", requiredMode = Schema.RequiredMode.REQUIRED)
    private DeliveryBoyVerificationStatus verificationStatus;

    @Schema(description = "Optional reason or audit remark if rejecting", example = "Driving license illegible")
    private String remarks;
}
