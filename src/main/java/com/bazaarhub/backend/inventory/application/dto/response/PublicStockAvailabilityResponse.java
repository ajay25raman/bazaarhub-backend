package com.bazaarhub.backend.inventory.application.dto.response;

import com.bazaarhub.backend.inventory.domain.model.StockAvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public product variant stock availability")
public class PublicStockAvailabilityResponse {

    @Schema(description = "Product Variant ID", example = "101")
    private Long variantId;

    @Schema(description = "Stock Keeping Unit", example = "IPHONE15-PROMAX-256-NAT")
    private String sku;

    @Schema(description = "Availability status: IN_STOCK, LOW_STOCK, OUT_OF_STOCK", example = "IN_STOCK")
    private StockAvailabilityStatus status;

    @Schema(description = "Whether the variant can currently be purchased online", example = "true")
    private boolean purchasable;
}
