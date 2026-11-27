package com.bazaarhub.backend.inventory.application.service;

import com.bazaarhub.backend.catalog.domain.exception.ProductNotFoundException;
import com.bazaarhub.backend.catalog.domain.exception.ProductVariantNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.inventory.application.dto.response.PublicStockAvailabilityResponse;
import com.bazaarhub.backend.inventory.application.mapper.InventoryMapper;
import com.bazaarhub.backend.inventory.domain.model.StockAvailabilityStatus;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicInventoryService {

    private final SpringDataProductRepository productRepository;
    private final SpringDataProductVariantRepository variantRepository;
    private final SpringDataInventoryRepository inventoryRepository;

    public PublicInventoryService(SpringDataProductRepository productRepository,
                                  SpringDataProductVariantRepository variantRepository,
                                  SpringDataInventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Resolves public availability status for an approved, active product SKU variant.
     * Guarantees zero leakage of internal warehouse counts or safety thresholds.
     */
    @Transactional(readOnly = true)
    public PublicStockAvailabilityResponse getPublicVariantAvailability(Long productId, Long variantId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        // Public visibility rule: Product must be ACTIVE and APPROVED
        if (product.getStatus() != ProductStatus.ACTIVE || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            throw new ProductNotFoundException("Product is not currently available for public view: " + productId);
        }

        ProductVariantEntity variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductVariantNotFoundException(
                        "Variant ID " + variantId + " not found on product ID " + productId));

        InventoryEntity inventory = inventoryRepository.findByVariantId(variantId).orElse(null);

        int available = inventory != null ? inventory.getAvailableQuantity() : 0;
        boolean isVariantActive = variant.getStatus() == CatalogStatus.ACTIVE;
        boolean purchasable = isVariantActive && available > 0;

        StockAvailabilityStatus status;
        if (!purchasable || available <= 0) {
            status = StockAvailabilityStatus.OUT_OF_STOCK;
        } else if (inventory != null && inventory.isLowStock()) {
            status = StockAvailabilityStatus.LOW_STOCK;
        } else {
            status = StockAvailabilityStatus.IN_STOCK;
        }

        return PublicStockAvailabilityResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .status(status)
                .purchasable(purchasable)
                .build();
    }
}
