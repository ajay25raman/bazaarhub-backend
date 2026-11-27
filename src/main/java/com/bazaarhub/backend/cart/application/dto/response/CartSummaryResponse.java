package com.bazaarhub.backend.cart.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "High-level cart summary and totals")
public class CartSummaryResponse {

    @Schema(description = "Total quantity of items in the cart", example = "3")
    private int totalItems;

    @Schema(description = "Count of distinct product variants in the cart", example = "2")
    private int totalUniqueItems;

    @Schema(description = "Cart subtotal amount", example = "379800.00")
    private BigDecimal subtotal;

    @Schema(description = "Applicable tax amount", example = "0.00")
    private BigDecimal taxAmount;

    @Schema(description = "Delivery and shipping fee", example = "0.00")
    private BigDecimal deliveryFee;

    @Schema(description = "Grand total payable amount", example = "379800.00")
    private BigDecimal grandTotal;

    @Builder.Default
    @Schema(description = "Seller-wise subtotal breakdown")
    private List<SellerSubtotalDto> sellers = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Brief seller subtotal breakdown item")
    public static class SellerSubtotalDto {
        private Long sellerId;
        private String businessName;
        private int itemCount;
        private BigDecimal subtotal;
    }
}
