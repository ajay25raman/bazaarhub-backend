package com.bazaarhub.backend.catalog.application.dto.response;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product variant response representation")
public class ProductVariantResponse {

    @Schema(description = "Variant record ID", example = "201")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Unique SKU", example = "IPHONE15-PROMAX-256-NAT")
    private String sku;

    @Schema(description = "Barcode / UPC / EAN", example = "195949012345")
    private String barcode;

    @Schema(description = "Variant name", example = "Natural Titanium - 256GB")
    private String variantName;

    @Schema(description = "Variant attributes JSON string", example = "{\"color\": \"Natural Titanium\", \"storage\": \"256GB\"}")
    private String attributes;

    @Schema(description = "Selling price", example = "134900.00")
    private BigDecimal sellingPrice;

    @Schema(description = "Maximum Retail Price (MRP)", example = "159900.00")
    private BigDecimal mrp;

    @Schema(description = "Applicable tax rate %", example = "18.00")
    private BigDecimal taxRate;

    @Schema(description = "Weight in kg", example = "0.221")
    private BigDecimal weight;

    @Schema(description = "Variant lifecycle status (ACTIVE, INACTIVE)", example = "ACTIVE")
    private CatalogStatus status;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:00:00Z")
    private Instant updatedAt;
}
