package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Request to complete delivery upon customer verification")
public class CompleteDeliveryRequest {

    @NotBlank(message = "Delivery verification OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "Delivery OTP must be exactly 6 digits")
    @Schema(description = "6-digit OTP received from customer", example = "584920", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otp;

    @Schema(description = "Optional Cloudinary URL of proof of delivery photo", example = "https://res.cloudinary.com/demo/image/upload/bazaarhub/delivery/proofs/delivery_123.jpg")
    private String deliveryProofUrl;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Delivery handover notes", example = "Handed directly to recipient at doorstep")
    private String notes;

    @DecimalMin(value = "0.0", inclusive = true, message = "Collected COD amount cannot be negative")
    @Schema(description = "Cash collected if order is Cash on Delivery (COD)", example = "1499.00")
    private BigDecimal codAmountCollected;
}
