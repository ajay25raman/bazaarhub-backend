package com.bazaarhub.backend.inventory.application.service;

import com.bazaarhub.backend.catalog.domain.exception.ProductVariantNotFoundException;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.StockReserveRequest;
import com.bazaarhub.backend.inventory.application.dto.response.StockReservationResponse;
import com.bazaarhub.backend.inventory.application.mapper.InventoryMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private static final int DEFAULT_RESERVATION_TTL_MINUTES = 15;

    private final SpringDataStockReservationRepository reservationRepository;
    private final SpringDataInventoryRepository inventoryRepository;
    private final SpringDataProductVariantRepository variantRepository;
    private final SpringDataUserRepository userRepository;
    private final InventoryService inventoryService;

    public StockReservationService(SpringDataStockReservationRepository reservationRepository,
                                   SpringDataInventoryRepository inventoryRepository,
                                   SpringDataProductVariantRepository variantRepository,
                                   SpringDataUserRepository userRepository,
                                   InventoryService inventoryService) {
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Reserves stock for a given checkout reference.
     * Safe under concurrency via pessimistic write lock on the inventory row.
     * Idempotent if called repeatedly with the same checkout reference and quantity.
     */
    @Transactional
    public StockReservationResponse reserveStock(StockReserveRequest request, Long userId) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidStockOperationException("Reservation quantity must be strictly greater than zero");
        }

        String checkoutRef = request.getCheckoutReference().trim();
        Long variantId = request.getVariantId();

        // 1. Check for existing reservation (Unique constraint: checkout_reference, variant_id)
        Optional<StockReservationEntity> existing = reservationRepository
                .findByCheckoutReferenceAndVariantId(checkoutRef, variantId);

        if (existing.isPresent()) {
            StockReservationEntity res = existing.get();
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                if (res.getQuantity().equals(request.getQuantity())) {
                    log.info("[RESERVATION IDEMPOTENT] Duplicate active reservation request for checkout: {}, variant: {}. Returning existing.",
                            checkoutRef, variantId);
                    return InventoryMapper.toReservationResponse(res);
                }
                throw new InvalidReservationStateException(String.format(
                        "Active reservation already exists for checkout '%s' with quantity %d, requested %d",
                        checkoutRef, res.getQuantity(), request.getQuantity()));
            }
            if (res.getStatus() == ReservationStatus.CONFIRMED) {
                throw new InvalidReservationStateException(
                        "Reservation has already been CONFIRMED for checkout: " + checkoutRef);
            }
            throw new InvalidReservationStateException(String.format(
                    "Reservation is already in terminal state %s for checkout: %s", res.getStatus(), checkoutRef));
        }

        UserEntity user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        // 2. Acquire pessimistic write lock on inventory
        InventoryEntity inventory = inventoryService.getOrCreateInventoryWithLock(variantId, user);
        ProductVariantEntity variant = inventory.getVariant();

        // 3. Verify stock availability
        int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (available < request.getQuantity()) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for SKU '%s'. Requested: %d, Available: %d (Total: %d, Reserved: %d)",
                    variant.getSku(), request.getQuantity(), available,
                    inventory.getTotalQuantity(), inventory.getReservedQuantity()));
        }

        // 4. Update reserved quantity on inventory
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
        if (user != null) {
            inventory.setUpdatedBy(user);
        }
        inventoryRepository.save(inventory);

        // 5. Create active reservation
        int ttlMinutes = (request.getTtlMinutes() != null && request.getTtlMinutes() > 0)
                ? request.getTtlMinutes()
                : DEFAULT_RESERVATION_TTL_MINUTES;

        StockReservationEntity reservation = StockReservationEntity.builder()
                .variant(variant)
                .checkoutReference(checkoutRef)
                .quantity(request.getQuantity())
                .status(ReservationStatus.ACTIVE)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)))
                .createdBy(user)
                .updatedBy(user)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        StockReservationEntity savedReservation = reservationRepository.save(reservation);

        // 6. Record movement
        inventoryService.recordMovement(inventory, MovementType.RESERVE, request.getQuantity(),
                "CHECKOUT", savedReservation.getId(),
                "Stock reserved for checkout reference: " + checkoutRef, user);

        log.info("[STOCK RESERVED] Locked {} units for checkout: '{}' on variant ID: {}. Expires at: {}",
                request.getQuantity(), checkoutRef, variantId, savedReservation.getExpiresAt());

        return InventoryMapper.toReservationResponse(savedReservation);
    }

    /**
     * Confirms an active reservation upon successful checkout/payment.
     * Decrements BOTH total and reserved quantity exactly once.
     */
    @Transactional
    public StockReservationResponse confirmReservation(String checkoutReference, Long variantId, Long userId) {
        StockReservationEntity reservation = reservationRepository
                .findByCheckoutReferenceAndVariantId(checkoutReference.trim(), variantId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found for checkout: " + checkoutReference + " and variant ID: " + variantId));

        // Idempotency: if already CONFIRMED, return without re-deducting stock
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            log.info("[RESERVATION CONFIRM IDEMPOTENT] Reservation already confirmed for checkout: {}", checkoutReference);
            return InventoryMapper.toReservationResponse(reservation);
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(String.format(
                    "Cannot confirm reservation in status %s for checkout: %s",
                    reservation.getStatus(), checkoutReference));
        }

        if (reservation.isExpired()) {
            throw new InvalidReservationStateException(String.format(
                    "Reservation has expired at %s for checkout: %s. Cannot confirm.",
                    reservation.getExpiresAt(), checkoutReference));
        }

        UserEntity user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        // Atomically transition reservation state
        int updated = reservationRepository.transitionStatusSafely(
                reservation.getId(), ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED, Instant.now());

        if (updated == 0) {
            throw new InvalidReservationStateException(
                    "Reservation was updated concurrently. Cannot confirm checkout: " + checkoutReference);
        }

        // Lock inventory and reduce total + reserved quantity
        InventoryEntity inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Inventory not found for variant ID: " + variantId));

        inventory.setTotalQuantity(inventory.getTotalQuantity() - reservation.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        if (user != null) {
            inventory.setUpdatedBy(user);
        }
        inventoryRepository.save(inventory);

        // Record STOCK_OUT movement
        inventoryService.recordMovement(inventory, MovementType.STOCK_OUT, reservation.getQuantity(),
                "CHECKOUT_CONFIRMED", reservation.getId(),
                "Stock permanently deducted upon checkout confirmation: " + checkoutReference, user);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setUpdatedAt(Instant.now());

        log.info("[STOCK CONFIRMED] Confirmed reservation ID: {} for checkout: '{}'. Deducted {} units from variant ID: {}",
                reservation.getId(), checkoutReference, reservation.getQuantity(), variantId);

        return InventoryMapper.toReservationResponse(reservation);
    }

    /**
     * Releases an active reservation (e.g. checkout abandoned, payment failed).
     * Reduces reserved quantity exactly once.
     */
    @Transactional
    public StockReservationResponse releaseReservation(String checkoutReference, Long variantId, Long userId, String reason) {
        StockReservationEntity reservation = reservationRepository
                .findByCheckoutReferenceAndVariantId(checkoutReference.trim(), variantId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found for checkout: " + checkoutReference + " and variant ID: " + variantId));

        // Idempotency: if already RELEASED, return without re-releasing
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            log.info("[RESERVATION RELEASE IDEMPOTENT] Reservation already released for checkout: {}", checkoutReference);
            return InventoryMapper.toReservationResponse(reservation);
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException(
                    "Cannot release already confirmed reservation for checkout: " + checkoutReference);
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(String.format(
                    "Cannot release reservation in status %s for checkout: %s",
                    reservation.getStatus(), checkoutReference));
        }

        UserEntity user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        // Atomically transition status
        int updated = reservationRepository.transitionStatusSafely(
                reservation.getId(), ReservationStatus.ACTIVE, ReservationStatus.RELEASED, Instant.now());

        if (updated == 0) {
            throw new InvalidReservationStateException(
                    "Reservation was updated concurrently. Cannot release checkout: " + checkoutReference);
        }

        // Lock inventory and reduce reserved quantity
        InventoryEntity inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Inventory not found for variant ID: " + variantId));

        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        if (user != null) {
            inventory.setUpdatedBy(user);
        }
        inventoryRepository.save(inventory);

        // Record RELEASE movement
        String notes = "Stock reservation released for checkout: " + checkoutReference +
                (reason != null && !reason.isBlank() ? " (Reason: " + reason.trim() + ")" : "");

        inventoryService.recordMovement(inventory, MovementType.RELEASE, reservation.getQuantity(),
                "CHECKOUT_RELEASED", reservation.getId(), notes, user);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setUpdatedAt(Instant.now());

        log.info("[STOCK RELEASED] Released reservation ID: {} for checkout: '{}'. Freed {} units on variant ID: {}",
                reservation.getId(), checkoutReference, reservation.getQuantity(), variantId);

        return InventoryMapper.toReservationResponse(reservation);
    }

    /**
     * Cancels an active reservation.
     */
    @Transactional
    public StockReservationResponse cancelReservation(String checkoutReference, Long variantId, Long userId, String reason) {
        StockReservationEntity reservation = reservationRepository
                .findByCheckoutReferenceAndVariantId(checkoutReference.trim(), variantId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found for checkout: " + checkoutReference + " and variant ID: " + variantId));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return InventoryMapper.toReservationResponse(reservation);
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException("Cannot cancel confirmed reservation for checkout: " + checkoutReference);
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException("Cannot cancel reservation in status: " + reservation.getStatus());
        }

        UserEntity user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        int updated = reservationRepository.transitionStatusSafely(
                reservation.getId(), ReservationStatus.ACTIVE, ReservationStatus.CANCELLED, Instant.now());

        if (updated == 0) {
            throw new InvalidReservationStateException("Reservation was updated concurrently. Cannot cancel checkout: " + checkoutReference);
        }

        InventoryEntity inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Inventory not found for variant ID: " + variantId));

        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        if (user != null) {
            inventory.setUpdatedBy(user);
        }
        inventoryRepository.save(inventory);

        String notes = "Stock reservation cancelled for checkout: " + checkoutReference +
                (reason != null && !reason.isBlank() ? " (Reason: " + reason.trim() + ")" : "");

        inventoryService.recordMovement(inventory, MovementType.RELEASE, reservation.getQuantity(),
                "CHECKOUT_CANCELLED", reservation.getId(), notes, user);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(Instant.now());

        log.info("[STOCK CANCELLED] Cancelled reservation ID: {} for checkout: '{}'. Freed {} units on variant ID: {}",
                reservation.getId(), checkoutReference, reservation.getQuantity(), variantId);

        return InventoryMapper.toReservationResponse(reservation);
    }

    /**
     * Scheduled or manual sweep: finds active reservations whose expires_at has passed,
     * releases their reserved quantities, and marks status EXPIRED.
     * Prevents duplicate release via atomic conditional update.
     */
    @Transactional
    public int expirePendingReservations() {
        Instant now = Instant.now();
        List<StockReservationEntity> expiredList = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, now);

        if (expiredList.isEmpty()) {
            return 0;
        }

        log.info("[RESERVATION EXPIRY] Found {} active expired reservations to process", expiredList.size());
        int expiredCount = 0;

        for (StockReservationEntity res : expiredList) {
            try {
                // Atomic transition: ACTIVE -> EXPIRED
                int updated = reservationRepository.transitionStatusSafely(
                        res.getId(), ReservationStatus.ACTIVE, ReservationStatus.EXPIRED, now);

                if (updated == 1) {
                    Long variantId = res.getVariant().getId();
                    InventoryEntity inventory = inventoryRepository.findByVariantIdWithLock(variantId).orElse(null);

                    if (inventory != null) {
                        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - res.getQuantity()));
                        inventory.setUpdatedAt(now);
                        inventoryRepository.save(inventory);

                        inventoryService.recordMovement(inventory, MovementType.RELEASE, res.getQuantity(),
                                "CHECKOUT_EXPIRED", res.getId(),
                                "Reservation expired automatically (TTL elapsed)", null);
                    }
                    expiredCount++;
                    log.info("[EXPIRED RESERVATION] Successfully released expired reservation ID: {} (checkout: {})",
                            res.getId(), res.getCheckoutReference());
                }
            } catch (Exception ex) {
                log.error("[RESERVATION EXPIRY ERROR] Failed to process expiry for reservation ID: {}", res.getId(), ex);
            }
        }

        return expiredCount;
    }

    @Transactional(readOnly = true)
    public StockReservationResponse getReservation(String checkoutReference, Long variantId) {
        StockReservationEntity reservation = reservationRepository
                .findByCheckoutReferenceAndVariantId(checkoutReference.trim(), variantId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found for checkout: " + checkoutReference + " and variant ID: " + variantId));
        return InventoryMapper.toReservationResponse(reservation);
    }

    @Transactional(readOnly = true)
    public Page<StockReservationResponse> getReservations(ReservationStatus status, Pageable pageable) {
        if (status != null) {
            return reservationRepository.findByStatus(status, pageable)
                    .map(InventoryMapper::toReservationResponse);
        }
        return reservationRepository.findAll(pageable)
                .map(InventoryMapper::toReservationResponse);
    }
}
