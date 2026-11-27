package com.bazaarhub.backend.inventory;

import com.bazaarhub.backend.catalog.domain.exception.ProductVariantNotFoundException;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.SafetyStockUpdateRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockAdjustmentRequest;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryResponse;
import com.bazaarhub.backend.inventory.application.service.AdminInventoryService;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.domain.exception.InvalidStockOperationException;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryMovementRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInventoryServiceUnitTest {

    @Mock
    private SpringDataInventoryRepository inventoryRepository;
    @Mock
    private SpringDataInventoryMovementRepository movementRepository;
    @Mock
    private SpringDataProductVariantRepository variantRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private StockReservationService stockReservationService;

    private AdminInventoryService adminInventoryService;

    private UserEntity adminUser;
    private ProductVariantEntity variant;
    private InventoryEntity inventory;

    @BeforeEach
    void setUp() {
        adminInventoryService = new AdminInventoryService(
                inventoryRepository, movementRepository, variantRepository, userRepository, inventoryService, stockReservationService);

        adminUser = UserEntity.builder().id(1L).fullName("Super Admin").email("admin@bazaarhub.com").build();

        ProductEntity product = ProductEntity.builder().id(10L).name("Sony WH-1000XM5").build();
        variant = ProductVariantEntity.builder().id(101L).product(product).sku("SONY-WH-XM5-BLK").build();

        inventory = InventoryEntity.builder()
                .id(1L)
                .variant(variant)
                .totalQuantity(100)
                .reservedQuantity(20)
                .safetyStock(15)
                .version(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Admin executes authoritative stock adjustment with mandatory reason")
    void adminAdjustStock_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(inventoryService.getOrCreateInventoryWithLock(101L, adminUser)).thenReturn(inventory);
        when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Adjust from 100 to 110 (delta: +10)
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .newTotalQuantity(110)
                .reason("Platform stock audit found untracked pallet")
                .build();

        InventoryResponse response = adminInventoryService.adminAdjustStock(1L, 101L, request);

        assertNotNull(response);
        assertEquals(110, response.getTotalQuantity());
        verify(inventoryRepository).save(inventory);

        verify(inventoryService).recordMovement(eq(inventory), eq(MovementType.ADJUSTMENT), eq(10),
                eq("ADMIN_ADJUSTMENT"), isNull(), contains("Platform stock audit found untracked pallet"), eq(adminUser));
    }

    @Test
    @DisplayName("Admin stock adjustment fails when reason is missing")
    void adminAdjustStock_failsWhenReasonMissing() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .newTotalQuantity(120)
                .reason("   ")
                .build();

        assertThrows(InvalidStockOperationException.class,
                () -> adminInventoryService.adminAdjustStock(1L, 101L, request));
    }

    @Test
    @DisplayName("Admin updates safety stock threshold successfully")
    void adminUpdateSafetyStock_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(inventoryService.getOrCreateInventoryWithLock(101L, adminUser)).thenReturn(inventory);
        when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

        SafetyStockUpdateRequest request = SafetyStockUpdateRequest.builder().safetyStock(25).build();
        InventoryResponse response = adminInventoryService.adminUpdateSafetyStock(1L, 101L, request);

        assertNotNull(response);
        assertEquals(25, response.getSafetyStock());
    }

    @Test
    @DisplayName("Admin get inventory throws ProductVariantNotFoundException when variant doesn't exist")
    void getInventoryByVariantId_failsWhenVariantNotFound() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductVariantNotFoundException.class,
                () -> adminInventoryService.getInventoryByVariantId(999L));
    }
}
