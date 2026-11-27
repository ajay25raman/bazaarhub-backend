package com.bazaarhub.backend.search.application.dto.response;

import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public storefront product search and recommendation result item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product search and discovery item for customer storefront")
public class ProductSearchResultDto {

    @Schema(description = "Product unique identifier", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "Apple iPhone 15 Pro Max")
    private String name;

    @Schema(description = "SEO-friendly product slug", example = "apple-iphone-15-pro-max")
    private String slug;

    @Schema(description = "Product description snippet")
    private String description;

    @Schema(description = "Category metadata")
    private CategoryInfo category;

    @Schema(description = "Brand metadata (null if unbranded)")
    private BrandInfo brand;

    @Schema(description = "Public seller info")
    private SellerInfo seller;

    @Schema(description = "Product status", example = "ACTIVE")
    private ProductStatus status;

    @Schema(description = "Approval status", example = "APPROVED")
    private ProductApprovalStatus approvalStatus;

    @Schema(description = "Primary hero image URL", example = "https://cdn.bazaarhub.com/iphone-front.webp")
    private String primaryImageUrl;

    @Schema(description = "Minimum selling price among active variants", example = "134900.00")
    private BigDecimal minPrice;

    @Schema(description = "Maximum selling price among active variants", example = "159900.00")
    private BigDecimal maxPrice;

    @Schema(description = "Total number of variants configured", example = "3")
    private int totalVariants;

    @Schema(description = "Whether product has stock available across any active variant", example = "true")
    private boolean inStock;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:00:00Z")
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Brief category information")
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Brief brand information")
    public static class BrandInfo {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Brief public seller information")
    public static class SellerInfo {
        private Long id;
        private String businessName;
    }
}
