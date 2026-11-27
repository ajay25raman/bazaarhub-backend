package com.bazaarhub.backend.wishlist.application.dto.response;

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
@Schema(description = "Customer wishlist product item response")
public class WishlistItemResponse {

    @Schema(description = "Wishlist Item ID", example = "501")
    private Long id;

    @Schema(description = "Wishlist ID", example = "10")
    private Long wishlistId;

    @Schema(description = "Catalog Product ID", example = "105")
    private Long productId;

    @Schema(description = "Product display title", example = "iPhone 15 Pro Max")
    private String productName;

    @Schema(description = "Product URL slug", example = "iphone-15-pro-max")
    private String productSlug;

    @Schema(description = "Brand name", example = "Apple")
    private String brandName;

    @Schema(description = "Category name", example = "Smartphones")
    private String categoryName;

    @Schema(description = "Base / starting price among active variants", example = "119999.00")
    private BigDecimal basePrice;

    @Schema(description = "Primary display image URL", example = "https://images.bazaarhub.com/products/iphone15pro.jpg")
    private String primaryImageUrl;

    @Schema(description = "Whether the product is active and has stock available", example = "true")
    private boolean inStock;

    @Schema(description = "Timestamp when item was added to wishlist", example = "2026-03-01T12:00:00Z")
    private Instant addedAt;
}
