package com.bazaarhub.backend.payment.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for executing a refund against an existing captured payment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to initiate full or partial refund for a captured payment")
public class CreateRefundRequest {

    @NotNull(message = "Refund amount is mandatory")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    @Schema(description = "Amount in INR to refund", example = "499.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal refundAmount;

    @NotBlank(message = "Refund reason is mandatory")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Schema(description = "Business justification or customer grievance for refund", example = "Damaged item received in transit", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
