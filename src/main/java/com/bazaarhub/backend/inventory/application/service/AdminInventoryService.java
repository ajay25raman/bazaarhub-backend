package com.bazaarhub.backend.inventory.application.service;

import com.bazaarhub.backend.catalog.domain.exception.ProductVariantNotFoundException;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.SafetyStockUpdateRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockAdjustmentRequest;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryMovementResponse;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryResponse;
import com.bazaarhub.backend.inventory.application.dto.response.StockReservationResponse;
import com.bazaarhub.backend.inventory.application.mapper.InventoryMapper;
import com.bazaarhub.backend.inventory.domain.exception.InvalidStockOperationException;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.domain.model.ReservationStatus;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryMovementRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminInventoryService {

    private static final Logger log = LoggerFactory.getLogger(AdminInventoryService.class);

    private final SpringDataInventoryRepository inventoryRepository;
    private final SpringDataInventoryMovementRepository movementRepository;
    private final SpringDataProductVariantRepository variantRepository;
    private final SpringDataUserRepository userRepository;
    private final InventoryService inventoryService;
    private final StockReservationService stockReservationService;

    public AdminInventoryService(SpringDataInventoryRepository inventoryRepository,
                                 SpringDataInventoryMovementRepository movementRepository,
                                 SpringDataProductVariantRepository variantRepository,
                                 SpringDataUserRepository userRepository,
                                 InventoryService inventoryService,
                                 StockReservationService stockReservationService) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.stockReservationService = stockReservationService;
    }

    /**
     * Admin lists platform-wide inventory with optional filtering by low/out-of-stock, seller, product, or keyword.
     */
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventory(String filter,
                                                   Long sellerId,
                                                   Long productId,
                                                   String search,
                                                   Pageable pageable) {
        Specification<InventoryEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<InventoryEntity, ProductVariantEntity> variantJoin = root.join("variant");
            Join<ProductVariantEntity, ProductEntity> productJoin = variantJoin.join("product");

            if (sellerId != null) {
                predicates.add(cb.equal(productJoin.get("seller").get("id"), sellerId));
            }

            if (productId != null) {
                predicates.add(cb.equal(productJoin.get("id"), productId));
            }

            jakarta.persistence.criteria.Expression<Integer> available = cb.diff(root.get("totalQuantity"), root.get("reservedQuantity"));
            if ("LOW_STOCK".equalsIgnoreCase(filter) || "low-stock".equalsIgnoreCase(filter)) {
                predicates.add(cb.lessThanOrEqualTo(available, root.get("safetyStock")));
            } else if ("OUT_OF_STOCK".equalsIgnoreCase(filter) || "out-of-stock".equalsIgnoreCase(filter)) {
                predicates.add(cb.lessThanOrEqualTo(available, 0));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate skuLike = cb.like(cb.lower(variantJoin.get("sku")), pattern);
                Predicate variantNameLike = cb.like(cb.lower(variantJoin.get("variantName")), pattern);
                Predicate productNameLike = cb.like(cb.lower(productJoin.get("name")), pattern);
                predicates.add(cb.or(skuLike, variantNameLike, productNameLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return inventoryRepository.findAll(spec, pageable).map(InventoryMapper::toResponse);
    }

    /**
     * Admin inspects inventory details for any SKU variant.
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByVariantId(Long variantId) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Variant not found with ID: " + variantId));

        InventoryEntity inventory = inventoryRepository.findByVariantId(variantId)
                .orElseGet(() -> inventoryService.initializeInventoryForVariant(variant, null));

        return InventoryMapper.toResponse(inventory);
    }

    /**
     * Admin executes an authoritative stock adjustment with a mandatory justification reason.
     */
    @Transactional
    public InventoryResponse adminAdjustStock(Long adminUserId, Long variantId, StockAdjustmentRequest request) {
        if (request.getNewTotalQuantity() == null || request.getNewTotalQuantity() < 0) {
            throw new InvalidStockOperationException("New total quantity cannot be null or negative");
        }

        if (request.getReason() == null || request.getReason().trim().isBlank()) {
            throw new InvalidStockOperationException("A mandatory documented reason is required for administrator stock adjustments");
        }

        UserEntity admin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
        InventoryEntity inventory = inventoryService.getOrCreateInventoryWithLock(variantId, admin);

        int oldTotal = inventory.getTotalQuantity();
        int newTotal = request.getNewTotalQuantity();

        if (newTotal == oldTotal) {
            throw new InvalidStockOperationException(String.format(
                    "Adjustment quantity (%d) must differ from current total quantity (%d)", newTotal, oldTotal));
        }

        if (newTotal < inventory.getReservedQuantity()) {
            throw new InvalidStockOperationException(String.format(
                    "Adjusted total (%d) cannot be less than currently reserved quantity (%d)",
                    newTotal, inventory.getReservedQuantity()));
        }

        int delta = newTotal - oldTotal;
        int movementQuantity = Math.abs(delta);

        inventory.setTotalQuantity(newTotal);
        if (admin != null) {
            inventory.setUpdatedBy(admin);
        }
        InventoryEntity saved = inventoryRepository.save(inventory);

        String auditNotes = String.format("ADMIN ADJUSTMENT: from %d to %d (delta: %+d). Reason: %s",
                oldTotal, newTotal, delta, request.getReason().trim());

        inventoryService.recordMovement(saved, MovementType.ADJUSTMENT, movementQuantity,
                "ADMIN_ADJUSTMENT", null, auditNotes, admin);

        log.info("[ADMIN INVENTORY ADJUSTMENT] Admin User ID: {} adjusted variant ID: {} stock from {} to {} (delta: %+d)",
                adminUserId, variantId, oldTotal, newTotal, delta);
        return InventoryMapper.toResponse(saved);
    }

    /**
     * Admin updates safety stock threshold for any variant.
     */
    @Transactional
    public InventoryResponse adminUpdateSafetyStock(Long adminUserId, Long variantId, SafetyStockUpdateRequest request) {
        if (request.getSafetyStock() < 0) {
            throw new InvalidStockOperationException("Safety stock cannot be negative: " + request.getSafetyStock());
        }

        UserEntity admin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
        InventoryEntity inventory = inventoryService.getOrCreateInventoryWithLock(variantId, admin);

        inventory.setSafetyStock(request.getSafetyStock());
        if (admin != null) {
            inventory.setUpdatedBy(admin);
        }
        InventoryEntity saved = inventoryRepository.save(inventory);

        log.info("[ADMIN SAFETY_STOCK] Admin User ID: {} set safety stock to {} on variant ID: {}",
                adminUserId, request.getSafetyStock(), variantId);
        return InventoryMapper.toResponse(saved);
    }

    /**
     * Admin retrieves movement history for a variant.
     */
    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getMovementsForVariant(Long variantId, Pageable pageable) {
        if (!variantRepository.existsById(variantId)) {
            throw new ProductVariantNotFoundException("Variant not found with ID: " + variantId);
        }

        return movementRepository.findByInventoryVariantId(variantId, pageable)
                .map(InventoryMapper::toMovementResponse);
    }

    /**
     * Admin inspects stock reservations across the platform.
     */
    @Transactional(readOnly = true)
    public Page<StockReservationResponse> getReservations(ReservationStatus status, Pageable pageable) {
        return stockReservationService.getReservations(status, pageable);
    }
}
