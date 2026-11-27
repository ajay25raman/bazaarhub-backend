package com.bazaarhub.backend.cart.application.dto.response;

import com.bazaarhub.backend.cart.domain.model.CartStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer shopping cart details, totals, and multi-vendor item groupings")
public class CartResponse {

    @Schema(description = "Cart ID", example = "1")
    private Long cartId;

    @Schema(description = "Cart lifecycle status", example = "ACTIVE")
    private CartStatus status;

    @Schema(description = "Total quantity of items in the cart", example = "3")
    private int totalItems;

    @Schema(description = "Count of distinct product variants in the cart", example = "2")
    private int totalUniqueItems;

    @Schema(description = "Subtotal before taxes and shipping", example = "379800.00")
    private BigDecimal subtotal;

    @Schema(description = "Computed applicable tax amount", example = "0.00")
    private BigDecimal taxAmount;

    @Schema(description = "Delivery and shipping fee", example = "0.00")
    private BigDecimal deliveryFee;

    @Schema(description = "Grand total payable amount", example = "379800.00")
    private BigDecimal grandTotal;

    @Builder.Default
    @Schema(description = "Cart items grouped by seller for multi-vendor visibility")
    private List<SellerCartGroupDto> sellers = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Flat list of all cart items")
    private List<CartItemResponse> items = new ArrayList<>();

    @Schema(description = "Cart creation timestamp")
    private Instant createdAt;

    @Schema(description = "Cart last updated timestamp")
    private Instant updatedAt;
}
