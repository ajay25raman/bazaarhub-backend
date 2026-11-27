package com.bazaarhub.backend.inventory.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Stock reduction / stock-out request payload")
public class StockOutRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of stock to remove (strictly positive)", example = "10")
    private Integer quantity;

    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    @Schema(description = "Source reference type (e.g. DAMAGE, SHRINKAGE, INTERNAL_TRANSFER)", example = "DAMAGED_GOODS")
    private String referenceType;

    @Schema(description = "Optional external reference ID", example = "501")
    private Long referenceId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Audit notes / reason for stock reduction", example = "Water damage in warehouse sector B")
    private String notes;
}
