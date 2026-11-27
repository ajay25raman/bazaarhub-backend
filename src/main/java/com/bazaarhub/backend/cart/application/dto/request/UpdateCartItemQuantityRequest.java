package com.bazaarhub.backend.cart.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
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
@Schema(description = "Request to update quantity of an existing item in the customer cart")
public class UpdateCartItemQuantityRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity cannot exceed 100 units per item")
    @Schema(description = "Updated item quantity (1 - 100)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
