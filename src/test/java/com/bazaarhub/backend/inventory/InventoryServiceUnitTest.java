package com.bazaarhub.backend.inventory;

import com.bazaarhub.backend.catalog.domain.exception.ProductVariantNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.SafetyStockUpdateRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockAdjustmentRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockInRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockOutRequest;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryResponse;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.domain.exception.InvalidStockOperationException;
import com.bazaarhub.backend.inventory.domain.exception.UnauthorizedInventoryAccessException;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.domain.model.StockAvailabilityStatus;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryMovementRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceUnitTest {

    @Mock
    private SpringDataInventoryRepository inventoryRepository;
    @Mock
    private SpringDataInventoryMovementRepository movementRepository;
    @Mock
    private SpringDataProductVariantRepository variantRepository;
    @Mock
    private SpringDataSellerRepository sellerRepository;
    @Mock
    private SpringDataUserRepository userRepository;

    private InventoryService inventoryService;

    private UserEntity sellerUser;
    private SellerEntity approvedSeller;
    private ProductEntity product;
    private ProductVariantEntity variant;
    private InventoryEntity sampleInventory;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                inventoryRepository, movementRepository, variantRepository, sellerRepository, userRepository);

        sellerUser = UserEntity.builder().id(10L).fullName("Seller Jane").email("jane@seller.com").build();

        approvedSeller = SellerEntity.builder()
                .id(100L)
                .user(sellerUser)
                .businessName("Jane Store")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        product = ProductEntity.builder()
                .id(500L)
                .seller(approvedSeller)
                .name("MacBook Pro 16")
                .slug("macbook-pro-16")
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build();

        variant = ProductVariantEntity.builder()
                .id(2001L)
                .product(product)
                .sku("MBP-16-M3")
                .variantName("Space Gray 36GB")
                .sellingPrice(new BigDecimal("2499.00"))
                .mrp(new BigDecimal("2699.00"))
                .status(CatalogStatus.ACTIVE)
                .build();

        sampleInventory = InventoryEntity.builder()
                .id(1L)
                .variant(variant)
                .totalQuantity(50)
                .reservedQuantity(10)
                .safetyStock(5)
                .version(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Inventory Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Safely initializes inventory with zero quantities if not present")
        void initializeInventory_createsWithZeroQuantities() {
            when(inventoryRepository.existsByVariantId(2001L)).thenReturn(false);
            when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> {
                InventoryEntity entity = i.getArgument(0);
                entity.setId(10L);
                return entity;
            });

            InventoryEntity result = inventoryService.initializeInventoryForVariant(variant, sellerUser);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals(0, result.getTotalQuantity());
            assertEquals(0, result.getReservedQuantity());
            assertEquals(0, result.getSafetyStock());
            assertEquals(0, result.getAvailableQuantity());
            assertEquals(StockAvailabilityStatus.OUT_OF_STOCK, result.calculateStatus());
            verify(inventoryRepository).save(any(InventoryEntity.class));
        }

        @Test
        @DisplayName("Returns existing inventory record if already initialized (idempotent)")
        void initializeInventory_idempotentWhenExists() {
            when(inventoryRepository.existsByVariantId(2001L)).thenReturn(true);
            when(inventoryRepository.findByVariantId(2001L)).thenReturn(Optional.of(sampleInventory));

            InventoryEntity result = inventoryService.initializeInventoryForVariant(variant, sellerUser);

            assertNotNull(result);
            assertEquals(50, result.getTotalQuantity());
            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Stock In Operations")
    class StockInTests {

        @Test
        @DisplayName("Stock-in increases total quantity and records STOCK_IN movement")
        void stockIn_success() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));
            when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
            when(inventoryRepository.findByVariantIdWithLock(2001L)).thenReturn(Optional.of(sampleInventory));
            when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

            StockInRequest request = StockInRequest.builder()
                    .quantity(25)
                    .referenceType("PURCHASE_ORDER")
                    .referenceId(101L)
                    .notes("Shipment received")
                    .build();

            InventoryResponse response = inventoryService.stockIn(10L, 2001L, request);

            assertNotNull(response);
            assertEquals(75, response.getTotalQuantity()); // 50 + 25
            assertEquals(10, response.getReservedQuantity());
            assertEquals(65, response.getAvailableQuantity()); // 75 - 10

            ArgumentCaptor<InventoryMovementEntity> movementCaptor = ArgumentCaptor.forClass(InventoryMovementEntity.class);
            verify(movementRepository).save(movementCaptor.capture());
            InventoryMovementEntity movement = movementCaptor.getValue();
            assertEquals(MovementType.STOCK_IN, movement.getMovementType());
            assertEquals(25, movement.getQuantity());
            assertEquals("PURCHASE_ORDER", movement.getReferenceType());
            assertEquals(101L, movement.getReferenceId());
        }

        @Test
        @DisplayName("Stock-in rejects zero or negative quantity")
        void stockIn_failsOnZeroOrNegativeQuantity() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));

            StockInRequest request = StockInRequest.builder().quantity(0).build();

            assertThrows(InvalidStockOperationException.class,
                    () -> inventoryService.stockIn(10L, 2001L, request));
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Stock-in rejects inactive product")
        void stockIn_failsOnInactiveProduct() {
            product.setStatus(ProductStatus.INACTIVE);
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));

            StockInRequest request = StockInRequest.builder().quantity(10).build();

            assertThrows(InvalidStockOperationException.class,
                    () -> inventoryService.stockIn(10L, 2001L, request));
            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Stock Out Operations")
    class StockOutTests {

        @Test
        @DisplayName("Stock-out decreases total quantity and records STOCK_OUT movement")
        void stockOut_success() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));
            when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
            when(inventoryRepository.findByVariantIdWithLock(2001L)).thenReturn(Optional.of(sampleInventory));
            when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

            // total = 50, reserved = 10, available = 40. Removing 15 -> new total = 35
            StockOutRequest request = StockOutRequest.builder()
                    .quantity(15)
                    .referenceType("DAMAGE")
                    .notes("Damaged in storage")
                    .build();

            InventoryResponse response = inventoryService.stockOut(10L, 2001L, request);

            assertNotNull(response);
            assertEquals(35, response.getTotalQuantity());
            assertEquals(10, response.getReservedQuantity());
            assertEquals(25, response.getAvailableQuantity()); // 35 - 10

            ArgumentCaptor<InventoryMovementEntity> movementCaptor = ArgumentCaptor.forClass(InventoryMovementEntity.class);
            verify(movementRepository).save(movementCaptor.capture());
            InventoryMovementEntity movement = movementCaptor.getValue();
            assertEquals(MovementType.STOCK_OUT, movement.getMovementType());
            assertEquals(15, movement.getQuantity());
        }

        @Test
        @DisplayName("Stock-out fails when attempting to remove more than available stock")
        void stockOut_failsWhenExceedsAvailable() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));
            when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
            when(inventoryRepository.findByVariantIdWithLock(2001L)).thenReturn(Optional.of(sampleInventory));

            // Available is 40 (50 total - 10 reserved). Attempting to remove 45 must fail.
            StockOutRequest request = StockOutRequest.builder().quantity(45).build();

            assertThrows(InsufficientStockException.class,
                    () -> inventoryService.stockOut(10L, 2001L, request));
            verify(movementRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Stock Adjustment Operations")
    class AdjustmentTests {

        @Test
        @DisplayName("Stock adjustment updates total count and records positive delta movement")
        void adjustStock_success() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));
            when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
            when(inventoryRepository.findByVariantIdWithLock(2001L)).thenReturn(Optional.of(sampleInventory));
            when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

            // Old total: 50. Adjusting to 65 (delta: +15)
            StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                    .newTotalQuantity(65)
                    .reason("Physical stock audit found extra boxes")
                    .build();

            InventoryResponse response = inventoryService.adjustStock(10L, 2001L, request);

            assertNotNull(response);
            assertEquals(65, response.getTotalQuantity());

            ArgumentCaptor<InventoryMovementEntity> captor = ArgumentCaptor.forClass(InventoryMovementEntity.class);
            verify(movementRepository).save(captor.capture());
            InventoryMovementEntity movement = captor.getValue();
            assertEquals(MovementType.ADJUSTMENT, movement.getMovementType());
            assertEquals(15, movement.getQuantity()); // strictly positive Math.abs(delta)
            assertTrue(movement.getNotes().contains("Reason: Physical stock audit found extra boxes"));
        }

        @Test
        @DisplayName("Stock adjustment fails when new total is below currently reserved stock")
        void adjustStock_failsWhenBelowReserved() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));
            when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
            when(inventoryRepository.findByVariantIdWithLock(2001L)).thenReturn(Optional.of(sampleInventory));

            // reserved is 10. Adjusting total to 5 must fail.
            StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                    .newTotalQuantity(5)
                    .reason("Count correction")
                    .build();

            assertThrows(InvalidStockOperationException.class,
                    () -> inventoryService.adjustStock(10L, 2001L, request));
            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("Stock adjustment fails when reason is blank")
        void adjustStock_failsWhenReasonBlank() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));

            StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                    .newTotalQuantity(40)
                    .reason("   ")
                    .build();

            assertThrows(InvalidStockOperationException.class,
                    () -> inventoryService.adjustStock(10L, 2001L, request));
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Stock adjustment fails when new total equals current total")
        void adjustStock_failsWhenQuantityUnchanged() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));
            when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
            when(inventoryRepository.findByVariantIdWithLock(2001L)).thenReturn(Optional.of(sampleInventory));

            StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                    .newTotalQuantity(50) // identical to sampleInventory total
                    .reason("Count correction")
                    .build();

            assertThrows(InvalidStockOperationException.class,
                    () -> inventoryService.adjustStock(10L, 2001L, request));
        }
    }

    @Nested
    @DisplayName("Ownership and Security Tests")
    class OwnershipTests {

        @Test
        @DisplayName("Seller cannot manage inventory for variant owned by another seller")
        void ownership_failsForOtherSellerVariant() {
            SellerEntity otherSeller = SellerEntity.builder().id(999L).build();
            product.setSeller(otherSeller); // variant belongs to seller 999, but actor is seller 100

            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));

            assertThrows(UnauthorizedInventoryAccessException.class,
                    () -> inventoryService.getSellerVariantInventory(10L, 2001L));
        }

        @Test
        @DisplayName("Unapproved seller cannot manage inventory")
        void ownership_failsForUnapprovedSeller() {
            approvedSeller.setApprovalStatus(SellerApprovalStatus.PENDING);
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));

            assertThrows(UnauthorizedInventoryAccessException.class,
                    () -> inventoryService.getSellerVariantInventory(10L, 2001L));
        }
    }

    @Nested
    @DisplayName("Safety Stock & Calculation Tests")
    class SafetyStockTests {

        @Test
        @DisplayName("Update safety stock threshold successfully")
        void updateSafetyStock_success() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));
            when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
            when(inventoryRepository.findByVariantIdWithLock(2001L)).thenReturn(Optional.of(sampleInventory));
            when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

            SafetyStockUpdateRequest request = SafetyStockUpdateRequest.builder().safetyStock(12).build();
            InventoryResponse response = inventoryService.updateSafetyStock(10L, 2001L, request);

            assertNotNull(response);
            assertEquals(12, response.getSafetyStock());
        }

        @Test
        @DisplayName("Safety stock cannot be negative")
        void updateSafetyStock_failsOnNegative() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(variantRepository.findById(2001L)).thenReturn(Optional.of(variant));

            SafetyStockUpdateRequest request = SafetyStockUpdateRequest.builder().safetyStock(-1).build();

            assertThrows(InvalidStockOperationException.class,
                    () -> inventoryService.updateSafetyStock(10L, 2001L, request));
        }
    }
}
