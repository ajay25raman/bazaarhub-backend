package com.bazaarhub.backend.inventory.application.dto.response;

import com.bazaarhub.backend.inventory.domain.model.StockAvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inventory stock details representation")
public class InventoryResponse {

    @Schema(description = "Inventory record ID", example = "10")
    private Long id;

    @Schema(description = "Variant ID", example = "101")
    private Long variantId;

    @Schema(description = "Variant SKU", example = "IPHONE15-PROMAX-256-NAT")
    private String sku;

    @Schema(description = "Variant Name", example = "Natural Titanium - 256GB")
    private String variantName;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product Name", example = "Apple iPhone 15 Pro Max")
    private String productName;

    @Schema(description = "Seller ID", example = "5")
    private Long sellerId;

    @Schema(description = "Seller Business Name", example = "Acme Electronics Ltd")
    private String sellerBusinessName;

    @Schema(description = "Total physical stock quantity", example = "100")
    private Integer totalQuantity;

    @Schema(description = "Quantity currently reserved for checkout", example = "10")
    private Integer reservedQuantity;

    @Schema(description = "Available quantity for purchase (total - reserved)", example = "90")
    private Integer availableQuantity;

    @Schema(description = "Threshold below which stock is marked as LOW_STOCK", example = "5")
    private Integer safetyStock;

    @Schema(description = "Calculated stock status: IN_STOCK, LOW_STOCK, OUT_OF_STOCK", example = "IN_STOCK")
    private StockAvailabilityStatus status;

    @Schema(description = "Optimistic locking version", example = "0")
    private Long version;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:00:00Z")
    private Instant updatedAt;
}
