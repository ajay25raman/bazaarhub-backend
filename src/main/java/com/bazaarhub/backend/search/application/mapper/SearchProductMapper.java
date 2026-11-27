package com.bazaarhub.backend.search.application.mapper;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.search.application.dto.response.ProductSearchResultDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Transforms ProductEntity into customer storefront Search and Discovery DTOs.
 * Ensures strict exclusion of internal, administrative, and private seller data.
 */
public final class SearchProductMapper {

    private SearchProductMapper() {
    }

    public static ProductSearchResultDto toSearchResult(ProductEntity entity, boolean inStock) {
        if (entity == null) {
            return null;
        }

        // 1. Resolve Primary Image URL
        String primaryImageUrl = null;
        if (entity.getImages() != null && !entity.getImages().isEmpty()) {
            primaryImageUrl = entity.getImages().stream()
                    .filter(ProductImageEntity::isPrimary)
                    .findFirst()
                    .map(ProductImageEntity::getImageUrl)
                    .orElseGet(() -> entity.getImages().get(0).getImageUrl());
        }

        // 2. Compute Price Boundaries across active variants
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        int activeVariantCount = 0;

        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            List<ProductVariantEntity> activeVariants = entity.getVariants().stream()
                    .filter(v -> v.getStatus() == CatalogStatus.ACTIVE)
                    .toList();

            activeVariantCount = activeVariants.size();

            minPrice = activeVariants.stream()
                    .map(ProductVariantEntity::getSellingPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

            maxPrice = activeVariants.stream()
                    .map(ProductVariantEntity::getSellingPrice)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);
        }

        // 3. Category info
        ProductSearchResultDto.CategoryInfo categoryInfo = null;
        if (entity.getCategory() != null) {
            categoryInfo = ProductSearchResultDto.CategoryInfo.builder()
                    .id(entity.getCategory().getId())
                    .name(entity.getCategory().getName())
                    .slug(entity.getCategory().getSlug())
                    .build();
        }

        // 4. Brand info
        ProductSearchResultDto.BrandInfo brandInfo = null;
        if (entity.getBrand() != null) {
            brandInfo = ProductSearchResultDto.BrandInfo.builder()
                    .id(entity.getBrand().getId())
                    .name(entity.getBrand().getName())
                    .slug(entity.getBrand().getSlug())
                    .build();
        }

        // 5. Public Seller info
        ProductSearchResultDto.SellerInfo sellerInfo = null;
        if (entity.getSeller() != null) {
            sellerInfo = ProductSearchResultDto.SellerInfo.builder()
                    .id(entity.getSeller().getId())
                    .businessName(entity.getSeller().getBusinessName())
                    .build();
        }

        return ProductSearchResultDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .category(categoryInfo)
                .brand(brandInfo)
                .seller(sellerInfo)
                .status(entity.getStatus())
                .approvalStatus(entity.getApprovalStatus())
                .primaryImageUrl(primaryImageUrl)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalVariants(activeVariantCount)
                .inStock(inStock)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ProductSearchResultDto toSearchResult(ProductEntity entity, Set<Long> inStockProductIds) {
        boolean inStock = inStockProductIds != null && inStockProductIds.contains(entity.getId());
        return toSearchResult(entity, inStock);
    }
}
