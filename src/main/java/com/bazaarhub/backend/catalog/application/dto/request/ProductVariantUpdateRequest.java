package com.bazaarhub.backend.catalog.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Product variant update request payload")
public class ProductVariantUpdateRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    @Schema(description = "Stock Keeping Unit identifier", example = "IPHONE15-PROMAX-256-NAT")
    private String sku;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    @Schema(description = "Barcode / UPC / EAN (optional)", example = "195949012345")
    private String barcode;

    @NotBlank(message = "Variant name is required")
    @Size(max = 150, message = "Variant name must not exceed 150 characters")
    @Schema(description = "Variant descriptive title", example = "Natural Titanium - 256GB - Special Edition")
    private String variantName;

    @Schema(description = "Variant attributes as a valid JSON string", example = "{\"color\": \"Natural Titanium\", \"storage\": \"256GB\"}")
    private String attributes;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
    @Schema(description = "Actual discounted selling price", example = "132900.00")
    private BigDecimal sellingPrice;

    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.01", message = "MRP must be greater than zero")
    @Schema(description = "Maximum Retail Price (must be greater than or equal to selling price)", example = "159900.00")
    private BigDecimal mrp;

    @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Tax rate cannot exceed 100%")
    @Schema(description = "Applicable tax percentage rate (0.00 - 100.00)", example = "18.00")
    private BigDecimal taxRate;

    @DecimalMin(value = "0.001", message = "Weight must be greater than zero")
    @Schema(description = "Gross item weight in kilograms", example = "0.221")
    private BigDecimal weight;
}
