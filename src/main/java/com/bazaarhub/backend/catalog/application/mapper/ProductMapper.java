package com.bazaarhub.backend.catalog.application.mapper;

import com.bazaarhub.backend.catalog.application.dto.response.ProductDetailResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductImageResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductSummaryResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductVariantResponse;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductSummaryResponse toSummary(ProductEntity entity) {
        if (entity == null) {
            return null;
        }

        // Primary image resolution
        String primaryImageUrl = null;
        if (entity.getImages() != null && !entity.getImages().isEmpty()) {
            primaryImageUrl = entity.getImages().stream()
                    .filter(ProductImageEntity::isPrimary)
                    .map(ProductImageEntity::getImageUrl)
                    .findFirst()
                    .orElse(entity.getImages().get(0).getImageUrl());
        }

        // Price range and variant count calculation
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        int totalVariants = 0;

        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            totalVariants = entity.getVariants().size();
            List<BigDecimal> activePrices = entity.getVariants().stream()
                    .filter(v -> v.getStatus() == CatalogStatus.ACTIVE)
                    .map(ProductVariantEntity::getSellingPrice)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

            if (!activePrices.isEmpty()) {
                minPrice = activePrices.get(0);
                maxPrice = activePrices.get(activePrices.size() - 1);
            }
        }

        ProductSummaryResponse.CategoryInfo categoryInfo = null;
        if (entity.getCategory() != null) {
            categoryInfo = ProductSummaryResponse.CategoryInfo.builder()
                    .id(entity.getCategory().getId())
                    .name(entity.getCategory().getName())
                    .slug(entity.getCategory().getSlug())
                    .build();
        }

        ProductSummaryResponse.BrandInfo brandInfo = null;
        if (entity.getBrand() != null) {
            brandInfo = ProductSummaryResponse.BrandInfo.builder()
                    .id(entity.getBrand().getId())
                    .name(entity.getBrand().getName())
                    .slug(entity.getBrand().getSlug())
                    .build();
        }

        ProductSummaryResponse.SellerInfo sellerInfo = null;
        if (entity.getSeller() != null) {
            sellerInfo = ProductSummaryResponse.SellerInfo.builder()
                    .id(entity.getSeller().getId())
                    .businessName(entity.getSeller().getBusinessName())
                    .build();
        }

        return ProductSummaryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .category(categoryInfo)
                .brand(brandInfo)
                .seller(sellerInfo)
                .status(entity.getStatus())
                .approvalStatus(entity.getApprovalStatus())
                .primaryImageUrl(primaryImageUrl)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalVariants(totalVariants)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ProductDetailResponse toDetail(ProductEntity entity) {
        if (entity == null) {
            return null;
        }

        ProductSummaryResponse.CategoryInfo categoryInfo = null;
        if (entity.getCategory() != null) {
            categoryInfo = ProductSummaryResponse.CategoryInfo.builder()
                    .id(entity.getCategory().getId())
                    .name(entity.getCategory().getName())
                    .slug(entity.getCategory().getSlug())
                    .build();
        }

        ProductSummaryResponse.BrandInfo brandInfo = null;
        if (entity.getBrand() != null) {
            brandInfo = ProductSummaryResponse.BrandInfo.builder()
                    .id(entity.getBrand().getId())
                    .name(entity.getBrand().getName())
                    .slug(entity.getBrand().getSlug())
                    .build();
        }

        ProductSummaryResponse.SellerInfo sellerInfo = null;
        if (entity.getSeller() != null) {
            sellerInfo = ProductSummaryResponse.SellerInfo.builder()
                    .id(entity.getSeller().getId())
                    .businessName(entity.getSeller().getBusinessName())
                    .build();
        }

        ProductDetailResponse.ReviewerInfo reviewerInfo = null;
        if (entity.getReviewedBy() != null) {
            reviewerInfo = ProductDetailResponse.ReviewerInfo.builder()
                    .id(entity.getReviewedBy().getId())
                    .email(entity.getReviewedBy().getEmail())
                    .build();
        }

        List<ProductImageResponse> imageResponses = Collections.emptyList();
        if (entity.getImages() != null && !entity.getImages().isEmpty()) {
            imageResponses = entity.getImages().stream()
                    .sorted(Comparator.comparingInt(ProductImageEntity::getDisplayOrder)
                            .thenComparing(ProductImageEntity::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(ProductMapper::toImageResponse)
                    .collect(Collectors.toList());
        }

        List<ProductVariantResponse> variantResponses = Collections.emptyList();
        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            variantResponses = entity.getVariants().stream()
                    .sorted(Comparator.comparing(ProductVariantEntity::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(ProductMapper::toVariantResponse)
                    .collect(Collectors.toList());
        }

        return ProductDetailResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .category(categoryInfo)
                .brand(brandInfo)
                .seller(sellerInfo)
                .status(entity.getStatus())
                .approvalStatus(entity.getApprovalStatus())
                .reviewedBy(reviewerInfo)
                .reviewedAt(entity.getReviewedAt())
                .rejectionReason(entity.getRejectionReason())
                .images(imageResponses)
                .variants(variantResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ProductImageResponse toImageResponse(ProductImageEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProductImageResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .imageUrl(entity.getImageUrl())
                .altText(entity.getAltText())
                .displayOrder(entity.getDisplayOrder())
                .isPrimary(entity.isPrimary())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ProductVariantResponse toVariantResponse(ProductVariantEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProductVariantResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .sku(entity.getSku())
                .barcode(entity.getBarcode())
                .variantName(entity.getVariantName())
                .attributes(entity.getAttributes())
                .sellingPrice(entity.getSellingPrice())
                .mrp(entity.getMrp())
                .taxRate(entity.getTaxRate())
                .weight(entity.getWeight())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
