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
@Schema(description = "Checkout stock reservation request payload")
public class StockReserveRequest {

    @NotNull(message = "Product variant ID is required")
    @Schema(description = "Target SKU variant ID", example = "101")
    private Long variantId;

    @NotBlank(message = "Checkout reference is required")
    @Size(max = 100, message = "Checkout reference must not exceed 100 characters")
    @Schema(description = "Unique checkout session identifier", example = "CHK-2026-9812-XYZ")
    private String checkoutReference;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Reservation quantity must be at least 1")
    @Schema(description = "Quantity of units to lock for checkout", example = "2")
    private Integer quantity;

    @Min(value = 1, message = "TTL must be at least 1 minute")
    @Schema(description = "Time-to-live in minutes before reservation auto-expires (default: 15 minutes)", example = "15")
    private Integer ttlMinutes;
}
