package com.bazaarhub.backend.inventory;

import com.bazaarhub.backend.catalog.domain.exception.ProductNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.inventory.application.dto.response.PublicStockAvailabilityResponse;
import com.bazaarhub.backend.inventory.application.service.PublicInventoryService;
import com.bazaarhub.backend.inventory.domain.model.StockAvailabilityStatus;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicInventoryServiceUnitTest {

    @Mock
    private SpringDataProductRepository productRepository;
    @Mock
    private SpringDataProductVariantRepository variantRepository;
    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    private PublicInventoryService publicInventoryService;

    private ProductEntity product;
    private ProductVariantEntity variant;
    private InventoryEntity inventory;

    @BeforeEach
    void setUp() {
        publicInventoryService = new PublicInventoryService(productRepository, variantRepository, inventoryRepository);

        product = ProductEntity.builder()
                .id(10L)
                .name("iPad Air")
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build();

        variant = ProductVariantEntity.builder()
                .id(101L)
                .product(product)
                .sku("IPAD-AIR-64-BLU")
                .status(CatalogStatus.ACTIVE)
                .build();

        inventory = InventoryEntity.builder()
                .id(1L)
                .variant(variant)
                .totalQuantity(50)
                .reservedQuantity(10)
                .safetyStock(5)
                .build();
    }

    @Test
    @DisplayName("Public availability returns IN_STOCK and purchasable=true for active product with available stock")
    void getPublicVariantAvailability_inStock() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(101L, 10L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantId(101L)).thenReturn(Optional.of(inventory));

        PublicStockAvailabilityResponse response = publicInventoryService.getPublicVariantAvailability(10L, 101L);

        assertNotNull(response);
        assertEquals(101L, response.getVariantId());
        assertEquals("IPAD-AIR-64-BLU", response.getSku());
        assertEquals(StockAvailabilityStatus.IN_STOCK, response.getStatus());
        assertTrue(response.isPurchasable());
    }

    @Test
    @DisplayName("Public availability returns LOW_STOCK when available stock is below safety stock")
    void getPublicVariantAvailability_lowStock() {
        inventory.setTotalQuantity(13);
        inventory.setReservedQuantity(10); // available is 3, safety stock is 5 -> LOW_STOCK

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(101L, 10L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantId(101L)).thenReturn(Optional.of(inventory));

        PublicStockAvailabilityResponse response = publicInventoryService.getPublicVariantAvailability(10L, 101L);

        assertNotNull(response);
        assertEquals(StockAvailabilityStatus.LOW_STOCK, response.getStatus());
        assertTrue(response.isPurchasable());
    }

    @Test
    @DisplayName("Public availability returns OUT_OF_STOCK and purchasable=false when stock is exhausted")
    void getPublicVariantAvailability_outOfStock() {
        inventory.setTotalQuantity(10);
        inventory.setReservedQuantity(10); // available is 0

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdAndProductId(101L, 10L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantId(101L)).thenReturn(Optional.of(inventory));

        PublicStockAvailabilityResponse response = publicInventoryService.getPublicVariantAvailability(10L, 101L);

        assertNotNull(response);
        assertEquals(StockAvailabilityStatus.OUT_OF_STOCK, response.getStatus());
        assertFalse(response.isPurchasable());
    }

    @Test
    @DisplayName("Throws ProductNotFoundException for unapproved product")
    void getPublicVariantAvailability_failsForUnapprovedProduct() {
        product.setApprovalStatus(ProductApprovalStatus.PENDING);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class,
                () -> publicInventoryService.getPublicVariantAvailability(10L, 101L));
    }

    @Test
    @DisplayName("Throws ProductNotFoundException for inactive product")
    void getPublicVariantAvailability_failsForInactiveProduct() {
        product.setStatus(ProductStatus.INACTIVE);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class,
                () -> publicInventoryService.getPublicVariantAvailability(10L, 101L));
    }
}
