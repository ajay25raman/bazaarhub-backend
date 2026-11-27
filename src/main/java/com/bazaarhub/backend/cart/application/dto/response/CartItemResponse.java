package com.bazaarhub.backend.cart.application.dto.response;

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
@Schema(description = "Cart item details with current pricing and stock verification")
public class CartItemResponse {

    @Schema(description = "Cart item ID", example = "1")
    private Long itemId;

    @Schema(description = "Product ID", example = "10")
    private Long productId;

    @Schema(description = "Product title", example = "Apple MacBook Pro 16")
    private String productName;

    @Schema(description = "Product URL slug", example = "apple-macbook-pro-16")
    private String productSlug;

    @Schema(description = "Product variant ID", example = "100")
    private Long variantId;

    @Schema(description = "Variant SKU code", example = "MBP16-M3-1TB")
    private String sku;

    @Schema(description = "Variant display name", example = "16-inch 1TB")
    private String variantName;

    @Schema(description = "Variant technical attributes (JSON format)", example = "{\"color\":\"Space Black\",\"ram\":\"36GB\"}")
    private String attributes;

    @Schema(description = "Primary product thumbnail URL", example = "https://cdn.bazaarhub.com/mbp16.webp")
    private String primaryImageUrl;

    @Schema(description = "Merchant/Seller ID", example = "1")
    private Long sellerId;

    @Schema(description = "Merchant business name", example = "Apex Retailers")
    private String sellerBusinessName;

    @Schema(description = "Current server-side selling price", example = "249900.00")
    private BigDecimal unitPrice;

    @Schema(description = "Maximum retail price (MRP)", example = "269900.00")
    private BigDecimal mrp;

    @Schema(description = "Item quantity in cart", example = "1")
    private int quantity;

    @Schema(description = "Item line subtotal (unitPrice * quantity)", example = "249900.00")
    private BigDecimal totalPrice;

    @Schema(description = "Whether the variant currently has sufficient available stock", example = "true")
    private boolean inStock;

    @Schema(description = "Current available stock count in warehouse", example = "18")
    private int availableStock;

    @Schema(description = "Whether the item price changed since added to cart", example = "false")
    private boolean priceChanged;

    @Schema(description = "Whether product and variant remain active, approved, and purchasable", example = "true")
    private boolean purchasable;
}
