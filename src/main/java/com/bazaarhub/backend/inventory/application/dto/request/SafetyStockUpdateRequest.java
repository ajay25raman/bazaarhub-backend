package com.bazaarhub.backend.inventory.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Safety stock threshold update request payload")
public class SafetyStockUpdateRequest {

    @NotNull(message = "Safety stock threshold is required")
    @Min(value = 0, message = "Safety stock cannot be negative")
    @Schema(description = "Minimum threshold quantity triggering low-stock alerts", example = "10")
    private Integer safetyStock;
}
