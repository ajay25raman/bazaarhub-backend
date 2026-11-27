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
@Schema(description = "Stock replenishment / stock-in request payload")
public class StockInRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of stock to add (strictly positive)", example = "50")
    private Integer quantity;

    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    @Schema(description = "Source reference type (e.g. PURCHASE_ORDER, RESTOCK, RETURN)", example = "MANUAL_RESTOCK")
    private String referenceType;

    @Schema(description = "Optional external reference ID", example = "10023")
    private Long referenceId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Optional context notes or batch information", example = "Received shipment from vendor batch #A1")
    private String notes;
}
