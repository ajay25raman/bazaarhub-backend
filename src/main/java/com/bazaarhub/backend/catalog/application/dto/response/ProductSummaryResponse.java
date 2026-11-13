package com.bazaarhub.backend.catalog.application.dto.response;

import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
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
@Schema(description = "Product summary response for catalog listings")
public class ProductSummaryResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "iPhone 15 Pro Max")
    private String name;

    @Schema(description = "Product URL slug", example = "iphone-15-pro-max")
    private String slug;

    @Schema(description = "Category summary")
    private CategoryInfo category;

    @Schema(description = "Brand summary (null if unbranded)")
    private BrandInfo brand;

    @Schema(description = "Seller summary")
    private SellerInfo seller;

    @Schema(description = "Product lifecycle status", example = "ACTIVE")
    private ProductStatus status;

    @Schema(description = "Product moderation approval status", example = "APPROVED")
    private ProductApprovalStatus approvalStatus;

    @Schema(description = "Primary hero image URL", example = "https://cdn.bazaarhub.com/products/iphone15-front.webp")
    private String primaryImageUrl;

    @Schema(description = "Minimum selling price among active variants", example = "134900.00")
    private BigDecimal minPrice;

    @Schema(description = "Maximum selling price among active variants", example = "199900.00")
    private BigDecimal maxPrice;

    @Schema(description = "Total number of variants configured", example = "3")
    private int totalVariants;

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
    @Schema(description = "Brief seller information")
    public static class SellerInfo {
        private Long id;
        private String businessName;
    }
}
