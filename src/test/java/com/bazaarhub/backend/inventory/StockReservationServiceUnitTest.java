package com.bazaarhub.backend.inventory;

import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.StockReserveRequest;
import com.bazaarhub.backend.inventory.application.dto.response.StockReservationResponse;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.domain.exception.InvalidReservationStateException;
import com.bazaarhub.backend.inventory.domain.exception.InvalidStockOperationException;
import com.bazaarhub.backend.inventory.domain.exception.ReservationNotFoundException;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.domain.model.ReservationStatus;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.StockReservationEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataStockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceUnitTest {

    @Mock
    private SpringDataStockReservationRepository reservationRepository;
    @Mock
    private SpringDataInventoryRepository inventoryRepository;
    @Mock
    private SpringDataProductVariantRepository variantRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private InventoryService inventoryService;

    private StockReservationService reservationService;

    private UserEntity customer;
    private ProductVariantEntity variant;
    private InventoryEntity inventory;
    private StockReservationEntity activeReservation;

    @BeforeEach
    void setUp() {
        reservationService = new StockReservationService(
                reservationRepository, inventoryRepository, variantRepository, userRepository, inventoryService);

        customer = UserEntity.builder().id(20L).fullName("Alice Customer").email("alice@test.com").build();

        variant = ProductVariantEntity.builder()
                .id(101L)
                .sku("IPHONE15-PRO-128")
                .variantName("iPhone 15 Pro 128GB")
                .build();

        inventory = InventoryEntity.builder()
                .id(1L)
                .variant(variant)
                .totalQuantity(20)
                .reservedQuantity(5)
                .safetyStock(2)
                .version(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        activeReservation = StockReservationEntity.builder()
                .id(501L)
                .variant(variant)
                .checkoutReference("CHK-TEST-001")
                .quantity(3)
                .status(ReservationStatus.ACTIVE)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .createdBy(customer)
                .updatedBy(customer)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Reserve Stock Tests")
    class ReserveTests {

        @Test
        @DisplayName("Successfully reserves stock when available")
        void reserveStock_success() {
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(20L)).thenReturn(Optional.of(customer));
            when(inventoryService.getOrCreateInventoryWithLock(101L, customer)).thenReturn(inventory);
            when(reservationRepository.save(any(StockReservationEntity.class))).thenAnswer(i -> {
                StockReservationEntity r = i.getArgument(0);
                r.setId(501L);
                return r;
            });

            StockReserveRequest request = StockReserveRequest.builder()
                    .variantId(101L)
                    .checkoutReference("CHK-TEST-001")
                    .quantity(4) // available is 15 (20 - 5)
                    .ttlMinutes(15)
                    .build();

            StockReservationResponse response = reservationService.reserveStock(request, 20L);

            assertNotNull(response);
            assertEquals(501L, response.getId());
            assertEquals("CHK-TEST-001", response.getCheckoutReference());
            assertEquals(4, response.getQuantity());
            assertEquals(ReservationStatus.ACTIVE, response.getStatus());

            // Reserved quantity increased by 4: 5 + 4 = 9
            assertEquals(9, inventory.getReservedQuantity());
            verify(inventoryRepository).save(inventory);

            verify(inventoryService).recordMovement(eq(inventory), eq(MovementType.RESERVE), eq(4),
                    eq("CHECKOUT"), eq(501L), contains("CHK-TEST-001"), eq(customer));
        }

        @Test
        @DisplayName("Rejects reservation when available quantity is insufficient")
        void reserveStock_insufficientStock() {
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(20L)).thenReturn(Optional.of(customer));
            when(inventoryService.getOrCreateInventoryWithLock(101L, customer)).thenReturn(inventory);

            // Available is 15 (20 - 5). Requesting 16 must fail.
            StockReserveRequest request = StockReserveRequest.builder()
                    .variantId(101L)
                    .checkoutReference("CHK-TEST-001")
                    .quantity(16)
                    .build();

            assertThrows(InsufficientStockException.class,
                    () -> reservationService.reserveStock(request, 20L));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Idempotent reservation returns existing reservation if duplicate with same quantity")
        void reserveStock_idempotentOnDuplicate() {
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));

            StockReserveRequest request = StockReserveRequest.builder()
                    .variantId(101L)
                    .checkoutReference("CHK-TEST-001")
                    .quantity(3) // matches activeReservation quantity
                    .build();

            StockReservationResponse response = reservationService.reserveStock(request, 20L);

            assertNotNull(response);
            assertEquals(501L, response.getId());
            assertEquals(ReservationStatus.ACTIVE, response.getStatus());
            verify(inventoryRepository, never()).save(any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects duplicate reservation if quantity differs")
        void reserveStock_failsOnDuplicateWithDifferentQuantity() {
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));

            StockReserveRequest request = StockReserveRequest.builder()
                    .variantId(101L)
                    .checkoutReference("CHK-TEST-001")
                    .quantity(5) // differs from activeReservation quantity (3)
                    .build();

            assertThrows(InvalidReservationStateException.class,
                    () -> reservationService.reserveStock(request, 20L));
        }

        @Test
        @DisplayName("Rejects reservation with zero or negative quantity")
        void reserveStock_failsOnNonPositiveQuantity() {
            StockReserveRequest request = StockReserveRequest.builder()
                    .variantId(101L)
                    .checkoutReference("CHK-TEST-001")
                    .quantity(0)
                    .build();

            assertThrows(InvalidStockOperationException.class,
                    () -> reservationService.reserveStock(request, 20L));
        }
    }

    @Nested
    @DisplayName("Confirm Reservation Tests")
    class ConfirmTests {

        @Test
        @DisplayName("Confirming reservation reduces BOTH total and reserved quantity exactly once")
        void confirmReservation_success() {
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));
            when(userRepository.findById(20L)).thenReturn(Optional.of(customer));
            when(reservationRepository.transitionStatusSafely(eq(501L), eq(ReservationStatus.ACTIVE), eq(ReservationStatus.CONFIRMED), any()))
                    .thenReturn(1);
            when(inventoryRepository.findByVariantIdWithLock(101L)).thenReturn(Optional.of(inventory));

            // Initial: total = 20, reserved = 5. Reservation qty = 3.
            StockReservationResponse response = reservationService.confirmReservation("CHK-TEST-001", 101L, 20L);

            assertNotNull(response);
            assertEquals(ReservationStatus.CONFIRMED, response.getStatus());

            // After confirmation: total = 20 - 3 = 17, reserved = 5 - 3 = 2
            assertEquals(17, inventory.getTotalQuantity());
            assertEquals(2, inventory.getReservedQuantity());
            verify(inventoryRepository).save(inventory);

            verify(inventoryService).recordMovement(eq(inventory), eq(MovementType.STOCK_OUT), eq(3),
                    eq("CHECKOUT_CONFIRMED"), eq(501L), contains("CHK-TEST-001"), eq(customer));
        }

        @Test
        @DisplayName("Confirmation is idempotent if already CONFIRMED")
        void confirmReservation_idempotentWhenAlreadyConfirmed() {
            activeReservation.setStatus(ReservationStatus.CONFIRMED);
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));

            StockReservationResponse response = reservationService.confirmReservation("CHK-TEST-001", 101L, 20L);

            assertNotNull(response);
            assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
            verify(inventoryRepository, never()).save(any());
            verify(inventoryService, never()).recordMovement(any(), any(), anyInt(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Cannot confirm expired reservation")
        void confirmReservation_failsWhenExpired() {
            activeReservation.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));

            assertThrows(InvalidReservationStateException.class,
                    () -> reservationService.confirmReservation("CHK-TEST-001", 101L, 20L));
            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Release & Cancel Reservation Tests")
    class ReleaseTests {

        @Test
        @DisplayName("Releasing reservation reduces reserved quantity exactly once and records RELEASE movement")
        void releaseReservation_success() {
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));
            when(userRepository.findById(20L)).thenReturn(Optional.of(customer));
            when(reservationRepository.transitionStatusSafely(eq(501L), eq(ReservationStatus.ACTIVE), eq(ReservationStatus.RELEASED), any()))
                    .thenReturn(1);
            when(inventoryRepository.findByVariantIdWithLock(101L)).thenReturn(Optional.of(inventory));

            // Initial: total = 20, reserved = 5. Reservation qty = 3.
            StockReservationResponse response = reservationService.releaseReservation(
                    "CHK-TEST-001", 101L, 20L, "Payment cancelled by user");

            assertNotNull(response);
            assertEquals(ReservationStatus.RELEASED, response.getStatus());

            // Reserved reduced by 3: 5 - 3 = 2. Total unchanged at 20.
            assertEquals(20, inventory.getTotalQuantity());
            assertEquals(2, inventory.getReservedQuantity());
            verify(inventoryRepository).save(inventory);

            verify(inventoryService).recordMovement(eq(inventory), eq(MovementType.RELEASE), eq(3),
                    eq("CHECKOUT_RELEASED"), eq(501L), contains("Payment cancelled by user"), eq(customer));
        }

        @Test
        @DisplayName("Release is idempotent if already RELEASED")
        void releaseReservation_idempotent() {
            activeReservation.setStatus(ReservationStatus.RELEASED);
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));

            StockReservationResponse response = reservationService.releaseReservation("CHK-TEST-001", 101L, 20L, "re-release");

            assertNotNull(response);
            assertEquals(ReservationStatus.RELEASED, response.getStatus());
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cannot release already confirmed reservation")
        void releaseReservation_failsIfConfirmed() {
            activeReservation.setStatus(ReservationStatus.CONFIRMED);
            when(reservationRepository.findByCheckoutReferenceAndVariantId("CHK-TEST-001", 101L))
                    .thenReturn(Optional.of(activeReservation));

            assertThrows(InvalidReservationStateException.class,
                    () -> reservationService.releaseReservation("CHK-TEST-001", 101L, 20L, "cancel"));
        }
    }

    @Nested
    @DisplayName("Expiry Processing Tests")
    class ExpiryTests {

        @Test
        @DisplayName("Expiry sweep releases reserved quantities and marks status EXPIRED")
        void expirePendingReservations_success() {
            StockReservationEntity expiredRes = StockReservationEntity.builder()
                    .id(601L)
                    .variant(variant)
                    .checkoutReference("CHK-EXPIRED-99")
                    .quantity(2)
                    .status(ReservationStatus.ACTIVE)
                    .expiresAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                    .build();

            when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(Instant.class)))
                    .thenReturn(List.of(expiredRes));
            when(reservationRepository.transitionStatusSafely(eq(601L), eq(ReservationStatus.ACTIVE), eq(ReservationStatus.EXPIRED), any()))
                    .thenReturn(1);
            when(inventoryRepository.findByVariantIdWithLock(101L)).thenReturn(Optional.of(inventory));

            int processed = reservationService.expirePendingReservations();

            assertEquals(1, processed);
            // Reserved reduced by 2: 5 - 2 = 3
            assertEquals(3, inventory.getReservedQuantity());
            verify(inventoryRepository).save(inventory);

            verify(inventoryService).recordMovement(eq(inventory), eq(MovementType.RELEASE), eq(2),
                    eq("CHECKOUT_EXPIRED"), eq(601L), contains("TTL elapsed"), isNull());
        }

        @Test
        @DisplayName("Expiry sweep returns 0 when no expired reservations exist")
        void expirePendingReservations_noneExpired() {
            when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(Instant.class)))
                    .thenReturn(List.of());

            int processed = reservationService.expirePendingReservations();

            assertEquals(0, processed);
            verify(inventoryRepository, never()).save(any());
        }
    }
}
