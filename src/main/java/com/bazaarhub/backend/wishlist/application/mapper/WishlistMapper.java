package com.bazaarhub.backend.wishlist.application.mapper;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistItemResponse;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistResponse;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistEntity;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistItemEntity;

import java.math.BigDecimal;
import java.util.Comparator;

public final class WishlistMapper {

    private WishlistMapper() {
    }

    public static WishlistResponse toWishlistResponse(WishlistEntity entity, int totalItems) {
        if (entity == null) {
            return null;
        }

        return WishlistResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .name(entity.getName())
                .totalItems(totalItems)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static WishlistItemResponse toItemResponse(WishlistItemEntity item, boolean inStock) {
        if (item == null) {
            return null;
        }

        ProductEntity product = item.getProduct();
        String brandName = (product != null && product.getBrand() != null) ? product.getBrand().getName() : null;
        String categoryName = (product != null && product.getCategory() != null) ? product.getCategory().getName() : null;

        String primaryImageUrl = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImageUrl = product.getImages().stream()
                    .min(Comparator.comparingInt(ProductImageEntity::getDisplayOrder))
                    .map(ProductImageEntity::getImageUrl)
                    .orElse(null);
        }

        BigDecimal basePrice = null;
        if (product != null && product.getVariants() != null && !product.getVariants().isEmpty()) {
            basePrice = product.getVariants().stream()
                    .filter(v -> v.getStatus() == CatalogStatus.ACTIVE)
                    .map(ProductVariantEntity::getSellingPrice)
                    .filter(p -> p != null)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
        }

        return WishlistItemResponse.builder()
                .id(item.getId())
                .wishlistId(item.getWishlist() != null ? item.getWishlist().getId() : null)
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productSlug(product != null ? product.getSlug() : null)
                .brandName(brandName)
                .categoryName(categoryName)
                .basePrice(basePrice)
                .primaryImageUrl(primaryImageUrl)
                .inStock(inStock)
                .addedAt(item.getCreatedAt())
                .build();
    }
}
