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
@Schema(description = "Request to add a product variant to the customer's active cart")
public class AddToCartRequest {

    @Schema(description = "Optional Product ID for validation", example = "10")
    private Long productId;

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product variant ID to add to cart", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity cannot exceed 100 units per item")
    @Builder.Default
    @Schema(description = "Quantity to add (1 - 100)", example = "1", defaultValue = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity = 1;
}
