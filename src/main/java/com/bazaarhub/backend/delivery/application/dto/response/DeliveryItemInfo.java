package com.bazaarhub.backend.delivery.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item details inside the delivered shipment")
public class DeliveryItemInfo {

    @Schema(description = "Product name", example = "Wireless Noise Cancelling Headphones")
    private String productName;

    @Schema(description = "SKU code", example = "BZH-HDPHN-BLK-01")
    private String sku;

    @Schema(description = "Quantity", example = "2")
    private int quantity;

    @Schema(description = "Unit price", example = "2499.00")
    private BigDecimal unitPrice;

    @Schema(description = "Total price", example = "4998.00")
    private BigDecimal totalPrice;
}
