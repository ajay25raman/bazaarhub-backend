package com.bazaarhub.backend.inventory.application.mapper;

import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryMovementResponse;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryResponse;
import com.bazaarhub.backend.inventory.application.dto.response.PublicStockAvailabilityResponse;
import com.bazaarhub.backend.inventory.application.dto.response.StockReservationResponse;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.StockReservationEntity;

public final class InventoryMapper {

    private InventoryMapper() {
    }

    public static InventoryResponse toResponse(InventoryEntity entity) {
        if (entity == null) {
            return null;
        }

        ProductVariantEntity variant = entity.getVariant();
        Long variantId = null;
        String sku = null;
        String variantName = null;
        Long productId = null;
        String productName = null;
        Long sellerId = null;
        String sellerBusinessName = null;

        if (variant != null) {
            variantId = variant.getId();
            sku = variant.getSku();
            variantName = variant.getVariantName();

            ProductEntity product = variant.getProduct();
            if (product != null) {
                productId = product.getId();
                productName = product.getName();
                if (product.getSeller() != null) {
                    sellerId = product.getSeller().getId();
                    sellerBusinessName = product.getSeller().getBusinessName();
                }
            }
        }

        return InventoryResponse.builder()
                .id(entity.getId())
                .variantId(variantId)
                .sku(sku)
                .variantName(variantName)
                .productId(productId)
                .productName(productName)
                .sellerId(sellerId)
                .sellerBusinessName(sellerBusinessName)
                .totalQuantity(entity.getTotalQuantity())
                .reservedQuantity(entity.getReservedQuantity())
                .availableQuantity(entity.getAvailableQuantity())
                .safetyStock(entity.getSafetyStock())
                .status(entity.calculateStatus())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static InventoryMovementResponse toMovementResponse(InventoryMovementEntity entity) {
        if (entity == null) {
            return null;
        }

        Long inventoryId = null;
        Long variantId = null;
        String sku = null;

        if (entity.getInventory() != null) {
            inventoryId = entity.getInventory().getId();
            if (entity.getInventory().getVariant() != null) {
                variantId = entity.getInventory().getVariant().getId();
                sku = entity.getInventory().getVariant().getSku();
            }
        }

        Long createdByUserId = entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null;

        return InventoryMovementResponse.builder()
                .id(entity.getId())
                .inventoryId(inventoryId)
                .variantId(variantId)
                .sku(sku)
                .movementType(entity.getMovementType())
                .quantity(entity.getQuantity())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .notes(entity.getNotes())
                .createdBy(createdByUserId)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static StockReservationResponse toReservationResponse(StockReservationEntity entity) {
        if (entity == null) {
            return null;
        }

        Long variantId = entity.getVariant() != null ? entity.getVariant().getId() : null;
        String sku = entity.getVariant() != null ? entity.getVariant().getSku() : null;

        return StockReservationResponse.builder()
                .id(entity.getId())
                .variantId(variantId)
                .sku(sku)
                .checkoutReference(entity.getCheckoutReference())
                .quantity(entity.getQuantity())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static PublicStockAvailabilityResponse toPublicAvailability(ProductVariantEntity variant,
                                                                       InventoryEntity inventory,
                                                                       boolean purchasable) {
        return PublicStockAvailabilityResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .status(inventory != null ? inventory.calculateStatus() : com.bazaarhub.backend.inventory.domain.model.StockAvailabilityStatus.OUT_OF_STOCK)
                .purchasable(purchasable)
                .build();
    }
}
