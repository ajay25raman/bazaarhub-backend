package com.bazaarhub.backend.inventory.application.service;

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
import com.bazaarhub.backend.inventory.application.dto.response.InventoryMovementResponse;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryResponse;
import com.bazaarhub.backend.inventory.application.mapper.InventoryMapper;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.domain.exception.InvalidStockOperationException;
import com.bazaarhub.backend.inventory.domain.exception.InventoryNotFoundException;
import com.bazaarhub.backend.inventory.domain.exception.UnauthorizedInventoryAccessException;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryMovementRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final SpringDataInventoryRepository inventoryRepository;
    private final SpringDataInventoryMovementRepository movementRepository;
    private final SpringDataProductVariantRepository variantRepository;
    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataUserRepository userRepository;

    public InventoryService(SpringDataInventoryRepository inventoryRepository,
                            SpringDataInventoryMovementRepository movementRepository,
                            SpringDataProductVariantRepository variantRepository,
                            SpringDataSellerRepository sellerRepository,
                            SpringDataUserRepository userRepository) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.variantRepository = variantRepository;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Initializes inventory for a newly created or uninitialized variant with zero stock.
     */
    @Transactional
    public InventoryEntity initializeInventoryForVariant(ProductVariantEntity variant, UserEntity actor) {
        if (inventoryRepository.existsByVariantId(variant.getId())) {
            return inventoryRepository.findByVariantId(variant.getId()).orElseThrow();
        }

        InventoryEntity inventory = InventoryEntity.builder()
                .variant(variant)
                .totalQuantity(0)
                .reservedQuantity(0)
                .safetyStock(0)
                .version(0L)
                .createdBy(actor)
                .updatedBy(actor)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        try {
            InventoryEntity saved = inventoryRepository.save(inventory);
            log.info("[INVENTORY INIT] Initialized inventory ID: {} for variant ID: {} (SKU: '{}')",
                    saved.getId(), variant.getId(), variant.getSku());
            return saved;
        } catch (Exception ex) {
            // In case of concurrent initialization, return existing
            log.warn("[INVENTORY INIT] Concurrent init caught for variant ID: {}. Returning existing record.", variant.getId());
            return inventoryRepository.findByVariantId(variant.getId()).orElseThrow();
        }
    }

    /**
     * Retrieves or creates inventory for a given variant ID.
     */
    @Transactional
    public InventoryEntity getOrCreateInventory(Long variantId, UserEntity actor) {
        return inventoryRepository.findByVariantId(variantId)
                .orElseGet(() -> {
                    ProductVariantEntity variant = variantRepository.findById(variantId)
                            .orElseThrow(() -> new ProductVariantNotFoundException("Variant not found with ID: " + variantId));
                    return initializeInventoryForVariant(variant, actor);
                });
    }

    /**
     * Retrieves or creates inventory with a pessimistic write lock for concurrent safety.
     */
    @Transactional
    public InventoryEntity getOrCreateInventoryWithLock(Long variantId, UserEntity actor) {
        return inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseGet(() -> {
                    ProductVariantEntity variant = variantRepository.findById(variantId)
                            .orElseThrow(() -> new ProductVariantNotFoundException("Variant not found with ID: " + variantId));
                    initializeInventoryForVariant(variant, actor);
                    return inventoryRepository.findByVariantIdWithLock(variantId)
                            .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for variant ID: " + variantId));
                });
    }

    /**
     * Seller views inventory details for their own product variant.
     */
    @Transactional(readOnly = true)
    public InventoryResponse getSellerVariantInventory(Long userId, Long variantId) {
        SellerEntity seller = resolveApprovedSeller(userId);
        ProductVariantEntity variant = getVariantAndValidateOwnership(variantId, seller);

        InventoryEntity inventory = inventoryRepository.findByVariantId(variantId)
                .orElseGet(() -> initializeInventoryForVariant(variant, seller.getUser()));

        return InventoryMapper.toResponse(inventory);
    }

    /**
     * Seller lists inventory across all their products with filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getSellerInventories(Long userId,
                                                         String filter,
                                                         String search,
                                                         Pageable pageable) {
        SellerEntity seller = resolveApprovedSeller(userId);

        Specification<InventoryEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<InventoryEntity, ProductVariantEntity> variantJoin = root.join("variant");
            Join<ProductVariantEntity, ProductEntity> productJoin = variantJoin.join("product");

            // Seller ownership constraint
            predicates.add(cb.equal(productJoin.get("seller").get("id"), seller.getId()));

            // Filter: LOW_STOCK, OUT_OF_STOCK
            jakarta.persistence.criteria.Expression<Integer> available = cb.diff(root.get("totalQuantity"), root.get("reservedQuantity"));
            if ("LOW_STOCK".equalsIgnoreCase(filter) || "low-stock".equalsIgnoreCase(filter)) {
                predicates.add(cb.lessThanOrEqualTo(available, root.get("safetyStock")));
            } else if ("OUT_OF_STOCK".equalsIgnoreCase(filter) || "out-of-stock".equalsIgnoreCase(filter)) {
                predicates.add(cb.lessThanOrEqualTo(available, 0));
            }

            // Keyword search across SKU, variant name, or product name
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
     * Seller updates the safety stock threshold for their variant.
     */
    @Transactional
    public InventoryResponse updateSafetyStock(Long userId, Long variantId, SafetyStockUpdateRequest request) {
        SellerEntity seller = resolveApprovedSeller(userId);
        ProductVariantEntity variant = getVariantAndValidateOwnership(variantId, seller);

        if (request.getSafetyStock() < 0) {
            throw new InvalidStockOperationException("Safety stock cannot be negative: " + request.getSafetyStock());
        }

        UserEntity user = resolveUser(userId);
        InventoryEntity inventory = getOrCreateInventoryWithLock(variantId, user);

        inventory.setSafetyStock(request.getSafetyStock());
        inventory.setUpdatedBy(user);
        InventoryEntity saved = inventoryRepository.save(inventory);

        log.info("[INVENTORY SAFETY_STOCK] Seller ID: {} updated safety stock to {} for variant ID: {}",
                seller.getId(), request.getSafetyStock(), variantId);
        return InventoryMapper.toResponse(saved);
    }

    /**
     * Seller restocks inventory (STOCK_IN).
     */
    @Transactional
    public InventoryResponse stockIn(Long userId, Long variantId, StockInRequest request) {
        SellerEntity seller = resolveApprovedSeller(userId);
        ProductVariantEntity variant = getVariantAndValidateOwnership(variantId, seller);
        validateProductEligibleForStockChange(variant.getProduct(), variant);

        if (request.getQuantity() <= 0) {
            throw new InvalidStockOperationException("Stock-in quantity must be strictly greater than zero");
        }

        UserEntity user = resolveUser(userId);
        InventoryEntity inventory = getOrCreateInventoryWithLock(variantId, user);

        inventory.setTotalQuantity(inventory.getTotalQuantity() + request.getQuantity());
        inventory.setUpdatedBy(user);
        InventoryEntity saved = inventoryRepository.save(inventory);

        recordMovement(saved, MovementType.STOCK_IN, request.getQuantity(),
                request.getReferenceType() != null ? request.getReferenceType() : "MANUAL_RESTOCK",
                request.getReferenceId(),
                request.getNotes() != null ? request.getNotes() : "Stock replenished by seller",
                user);

        log.info("[INVENTORY STOCK_IN] Added {} units to variant ID: {}. New total: {}",
                request.getQuantity(), variantId, saved.getTotalQuantity());
        return InventoryMapper.toResponse(saved);
    }

    /**
     * Seller removes inventory (STOCK_OUT).
     */
    @Transactional
    public InventoryResponse stockOut(Long userId, Long variantId, StockOutRequest request) {
        SellerEntity seller = resolveApprovedSeller(userId);
        ProductVariantEntity variant = getVariantAndValidateOwnership(variantId, seller);
        validateProductEligibleForStockChange(variant.getProduct(), variant);

        if (request.getQuantity() <= 0) {
            throw new InvalidStockOperationException("Stock-out quantity must be strictly greater than zero");
        }

        UserEntity user = resolveUser(userId);
        InventoryEntity inventory = getOrCreateInventoryWithLock(variantId, user);

        int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (request.getQuantity() > available) {
            throw new InsufficientStockException(String.format(
                    "Cannot remove %d units. Available unreserved stock is %d (Total: %d, Reserved: %d)",
                    request.getQuantity(), available, inventory.getTotalQuantity(), inventory.getReservedQuantity()));
        }

        inventory.setTotalQuantity(inventory.getTotalQuantity() - request.getQuantity());
        inventory.setUpdatedBy(user);
        InventoryEntity saved = inventoryRepository.save(inventory);

        recordMovement(saved, MovementType.STOCK_OUT, request.getQuantity(),
                request.getReferenceType() != null ? request.getReferenceType() : "MANUAL_REMOVAL",
                request.getReferenceId(),
                request.getNotes() != null ? request.getNotes() : "Stock removed by seller",
                user);

        log.info("[INVENTORY STOCK_OUT] Deducted {} units from variant ID: {}. New total: {}",
                request.getQuantity(), variantId, saved.getTotalQuantity());
        return InventoryMapper.toResponse(saved);
    }

    /**
     * Seller adjusts physical stock count with mandatory justification reason.
     */
    @Transactional
    public InventoryResponse adjustStock(Long userId, Long variantId, StockAdjustmentRequest request) {
        SellerEntity seller = resolveApprovedSeller(userId);
        ProductVariantEntity variant = getVariantAndValidateOwnership(variantId, seller);
        validateProductEligibleForStockChange(variant.getProduct(), variant);

        if (request.getNewTotalQuantity() == null || request.getNewTotalQuantity() < 0) {
            throw new InvalidStockOperationException("New total quantity cannot be null or negative");
        }

        if (request.getReason() == null || request.getReason().trim().isBlank()) {
            throw new InvalidStockOperationException("A mandatory documented reason is required for stock adjustments");
        }

        UserEntity user = resolveUser(userId);
        InventoryEntity inventory = getOrCreateInventoryWithLock(variantId, user);

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
        inventory.setUpdatedBy(user);
        InventoryEntity saved = inventoryRepository.save(inventory);

        String auditNotes = String.format("Adjustment from %d to %d (delta: %+d). Reason: %s",
                oldTotal, newTotal, delta, request.getReason().trim());

        recordMovement(saved, MovementType.ADJUSTMENT, movementQuantity,
                "MANUAL_ADJUSTMENT", null, auditNotes, user);

        log.info("[INVENTORY ADJUSTMENT] Adjusted variant ID: {} stock from {} to {} (delta: %+d) by seller ID: {}",
                variantId, oldTotal, newTotal, delta, seller.getId());
        return InventoryMapper.toResponse(saved);
    }

    /**
     * Seller retrieves movement history for their product variant.
     */
    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getSellerVariantMovements(Long userId, Long variantId, Pageable pageable) {
        SellerEntity seller = resolveApprovedSeller(userId);
        getVariantAndValidateOwnership(variantId, seller);

        return movementRepository.findByInventoryVariantId(variantId, pageable)
                .map(InventoryMapper::toMovementResponse);
    }

    /**
     * Creates an immutable inventory movement entry in the same transaction.
     */
    @Transactional
    public InventoryMovementEntity recordMovement(InventoryEntity inventory,
                                                  MovementType movementType,
                                                  int quantity,
                                                  String referenceType,
                                                  Long referenceId,
                                                  String notes,
                                                  UserEntity actor) {
        if (quantity <= 0) {
            throw new InvalidStockOperationException("Movement quantity must be strictly greater than zero");
        }

        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .inventory(inventory)
                .movementType(movementType)
                .quantity(quantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes(notes)
                .createdBy(actor)
                .updatedBy(actor)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return movementRepository.save(movement);
    }

    /**
     * Deletes inventory and its movements when a variant is deleted.
     */
    @Transactional
    public void deleteInventoryForVariant(Long variantId) {
        inventoryRepository.findByVariantId(variantId).ifPresent(inventory -> {
            movementRepository.findByInventoryId(inventory.getId(), Pageable.unpaged())
                    .forEach(movementRepository::delete);
            inventoryRepository.delete(inventory);
            log.info("[INVENTORY DELETE] Deleted inventory ID: {} for variant ID: {}", inventory.getId(), variantId);
        });
    }

    private ProductVariantEntity getVariantAndValidateOwnership(Long variantId, SellerEntity seller) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Variant not found with ID: " + variantId));

        ProductEntity product = variant.getProduct();
        if (product == null || product.getSeller() == null || !product.getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedInventoryAccessException(
                    "You do not have permission to manage inventory for variant ID: " + variantId);
        }

        return variant;
    }

    private void validateProductEligibleForStockChange(ProductEntity product, ProductVariantEntity variant) {
        if (product.getStatus() == ProductStatus.INACTIVE || product.getStatus() == ProductStatus.ARCHIVED) {
            throw new InvalidStockOperationException("Cannot modify stock for inactive or archived product: " + product.getName());
        }

        if (product.getApprovalStatus() == ProductApprovalStatus.REJECTED) {
            throw new InvalidStockOperationException("Cannot modify stock for rejected product: " + product.getName());
        }

        if (variant.getStatus() == CatalogStatus.INACTIVE) {
            throw new InvalidStockOperationException("Cannot modify stock for inactive variant: " + variant.getVariantName());
        }

        SellerEntity seller = product.getSeller();
        if (seller != null && (seller.getApprovalStatus() == SellerApprovalStatus.SUSPENDED
                || seller.getApprovalStatus() == SellerApprovalStatus.REJECTED)) {
            throw new UnauthorizedInventoryAccessException(
                    "Cannot modify stock while seller account is " + seller.getApprovalStatus());
        }
    }

    private SellerEntity resolveApprovedSeller(Long userId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedInventoryAccessException(
                        "No seller profile found for user ID: " + userId));

        if (seller.getApprovalStatus() != SellerApprovalStatus.APPROVED) {
            throw new UnauthorizedInventoryAccessException(
                    "Seller account is not approved. Current status: " + seller.getApprovalStatus());
        }

        return seller;
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedInventoryAccessException("User not found with ID: " + userId));
    }
}
