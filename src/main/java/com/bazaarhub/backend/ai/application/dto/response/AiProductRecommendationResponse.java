package com.bazaarhub.backend.ai.application.dto.response;

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
@Schema(description = "AI-powered product recommendation result with transparent contextual reason")
public class AiProductRecommendationResponse {

    @Schema(description = "Product ID", example = "42")
    private Long productId;

    @Schema(description = "Product display name", example = "Wireless Ergonomic Keyboard")
    private String name;

    @Schema(description = "SEO URL slug", example = "wireless-ergonomic-keyboard")
    private String slug;

    @Schema(description = "Category name", example = "Computer Accessories")
    private String categoryName;

    @Schema(description = "Brand name", example = "LogiTech")
    private String brandName;

    @Schema(description = "Minimum selling price across available variants", example = "4999.00")
    private BigDecimal price;

    @Schema(description = "Primary product image URL", example = "https://res.cloudinary.com/demo/image/upload/v1/keyboard.jpg")
    private String imageUrl;

    @Schema(description = "Transparent rationale for this recommendation without false personalization claims", example = "Trending in Computer Accessories with verified in-stock availability")
    private String recommendationReason;

    @Schema(description = "Whether the product is currently in-stock and purchasable", example = "true")
    private boolean inStock;

    @Schema(description = "Average customer star rating", example = "4.8")
    private Double averageRating;
}
