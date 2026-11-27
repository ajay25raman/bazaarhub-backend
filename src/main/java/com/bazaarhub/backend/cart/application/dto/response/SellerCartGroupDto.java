package com.bazaarhub.backend.cart.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Seller-wise grouping of cart items for multi-vendor marketplace fulfillment")
public class SellerCartGroupDto {

    @Schema(description = "Seller ID", example = "1")
    private Long sellerId;

    @Schema(description = "Seller business name", example = "Apex Retailers")
    private String businessName;

    @Schema(description = "Total number of items purchased from this seller", example = "2")
    private int itemCount;

    @Schema(description = "Subtotal for all items purchased from this seller", example = "379800.00")
    private BigDecimal subtotal;

    @Schema(description = "Items belonging to this seller")
    private List<CartItemResponse> items;
}
