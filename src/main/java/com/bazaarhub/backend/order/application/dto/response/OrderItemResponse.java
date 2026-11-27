package com.bazaarhub.backend.order.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Details of an ordered product variant item")
public class OrderItemResponse {

    @Schema(description = "Order item ID", example = "101")
    private Long id;

    @Schema(description = "Product variant ID", example = "5")
    private Long variantId;

    @Schema(description = "Product name at purchase time", example = "Apple iPhone 15 Pro")
    private String productName;

    @Schema(description = "Variant SKU at purchase time", example = "IP15P-128-NAT")
    private String variantSku;

    @Schema(description = "Variant attributes JSON string", example = "{\"color\":\"Natural Titanium\",\"storage\":\"128GB\"}")
    private String variantAttributes;

    @Schema(description = "Unit selling price", example = "129999.00")
    private BigDecimal unitPrice;

    @Schema(description = "Original MRP", example = "134999.00")
    private BigDecimal mrp;

    @Schema(description = "Tax rate percentage", example = "18.00")
    private BigDecimal taxRate;

    @Schema(description = "Tax amount per item line", example = "23399.82")
    private BigDecimal taxAmount;

    @Schema(description = "Purchased quantity", example = "1")
    private Integer quantity;

    @Schema(description = "Total item price (unitPrice * quantity)", example = "129999.00")
    private BigDecimal totalPrice;
}
