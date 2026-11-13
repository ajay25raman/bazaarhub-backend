package com.bazaarhub.backend.catalog.application.dto.response;

import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed product response with images and variants")
public class ProductDetailResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "iPhone 15 Pro Max")
    private String name;

    @Schema(description = "Product URL slug", example = "iphone-15-pro-max")
    private String slug;

    @Schema(description = "Detailed product description", example = "Titanium design with A17 Pro chip...")
    private String description;

    @Schema(description = "Associated category details")
    private ProductSummaryResponse.CategoryInfo category;

    @Schema(description = "Associated brand details (null if unbranded)")
    private ProductSummaryResponse.BrandInfo brand;

    @Schema(description = "Owning seller details")
    private ProductSummaryResponse.SellerInfo seller;

    @Schema(description = "Product lifecycle status", example = "ACTIVE")
    private ProductStatus status;

    @Schema(description = "Product moderation approval status", example = "APPROVED")
    private ProductApprovalStatus approvalStatus;

    @Schema(description = "Admin who reviewed the product (if reviewed)")
    private ReviewerInfo reviewedBy;

    @Schema(description = "Timestamp when the product was reviewed", example = "2026-11-13T11:00:00Z")
    private Instant reviewedAt;

    @Schema(description = "Rejection explanation (if rejected)", example = "null")
    private String rejectionReason;

    @Schema(description = "List of product images ordered by display order")
    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();

    @Schema(description = "List of product variants")
    @Builder.Default
    private List<ProductVariantResponse> variants = new ArrayList<>();

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:00:00Z")
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Reviewer admin info")
    public static class ReviewerInfo {
        private Long id;
        private String email;
    }
}
