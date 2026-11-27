package com.bazaarhub.backend.inventory.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Physical stock count adjustment request payload")
public class StockAdjustmentRequest {

    @NotNull(message = "New total quantity is required")
    @Min(value = 0, message = "New total quantity cannot be negative")
    @Schema(description = "Absolute target total stock count after physical audit (must be >= reservedQuantity)", example = "120")
    private Integer newTotalQuantity;

    @NotBlank(message = "Mandatory reason for stock adjustment is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Schema(description = "Mandatory documented justification for stock discrepancy", example = "Quarterly physical stock reconciliation")
    private String reason;
}
