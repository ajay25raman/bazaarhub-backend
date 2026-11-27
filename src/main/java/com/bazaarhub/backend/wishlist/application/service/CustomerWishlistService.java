package com.bazaarhub.backend.wishlist.application.service;

import com.bazaarhub.backend.catalog.domain.exception.ProductNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.wishlist.application.dto.request.WishlistUpdateRequest;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistItemResponse;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistProductExistsResponse;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistResponse;
import com.bazaarhub.backend.wishlist.application.mapper.WishlistMapper;
import com.bazaarhub.backend.wishlist.domain.exception.InvalidWishlistOperationException;
import com.bazaarhub.backend.wishlist.domain.exception.WishlistItemAlreadyExistsException;
import com.bazaarhub.backend.wishlist.domain.exception.WishlistItemNotFoundException;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistEntity;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistItemEntity;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.repository.SpringDataWishlistItemRepository;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.repository.SpringDataWishlistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CustomerWishlistService {

    private final SpringDataWishlistRepository wishlistRepository;
    private final SpringDataWishlistItemRepository wishlistItemRepository;
    private final SpringDataProductRepository productRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataInventoryRepository inventoryRepository;

    public CustomerWishlistService(SpringDataWishlistRepository wishlistRepository,
                                   SpringDataWishlistItemRepository wishlistItemRepository,
                                   SpringDataProductRepository productRepository,
                                   SpringDataUserRepository userRepository,
                                   SpringDataInventoryRepository inventoryRepository) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public WishlistResponse getCustomerWishlist(Long userId) {
        WishlistEntity wishlist = getOrCreateWishlist(userId);
        int totalItems = wishlistItemRepository.countByWishlistId(wishlist.getId());
        return WishlistMapper.toWishlistResponse(wishlist, totalItems);
    }

    @Transactional
    public WishlistResponse updateWishlistName(Long userId, WishlistUpdateRequest request) {
        WishlistEntity wishlist = getOrCreateWishlist(userId);
        UserEntity user = resolveUser(userId);

        wishlist.setName(request.getName().trim());
        wishlist.setUpdatedBy(user);
        WishlistEntity saved = wishlistRepository.save(wishlist);

        int totalItems = wishlistItemRepository.countByWishlistId(saved.getId());
        log.info("[WISHLIST RENAMED] Wishlist ID: {} renamed to '{}' for user ID: {}", saved.getId(), saved.getName(), userId);
        return WishlistMapper.toWishlistResponse(saved, totalItems);
    }

    @Transactional
    public Page<WishlistItemResponse> getWishlistItems(Long userId, Pageable pageable) {
        WishlistEntity wishlist = getOrCreateWishlist(userId);
        Page<WishlistItemEntity> page = wishlistItemRepository.findByWishlistIdOrderByCreatedAtDesc(wishlist.getId(), pageable);

        return page.map(item -> {
            boolean inStock = isProductInStock(item.getProduct());
            return WishlistMapper.toItemResponse(item, inStock);
        });
    }

    @Transactional
    public WishlistItemResponse addProductToWishlist(Long userId, Long productId) {
        WishlistEntity wishlist = getOrCreateWishlist(userId);
        UserEntity user = resolveUser(userId);

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        // Public eligibility verification: must be ACTIVE and APPROVED
        if (product.getStatus() != ProductStatus.ACTIVE || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            log.warn("[WISHLIST ADD REJECTED] Product ID {} is not eligible (status: {}, approval: {})",
                    productId, product.getStatus(), product.getApprovalStatus());
            throw new InvalidWishlistOperationException("Product ID " + productId + " is not currently available for public listing");
        }

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            throw new WishlistItemAlreadyExistsException(wishlist.getId(), productId);
        }

        WishlistItemEntity item = WishlistItemEntity.builder()
                .wishlist(wishlist)
                .product(product)
                .createdBy(user)
                .updatedBy(user)
                .build();

        WishlistItemEntity saved;
        try {
            saved = wishlistItemRepository.save(item);
            wishlistItemRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            log.warn("[CONCURRENT WISHLIST ADD] Unique constraint caught for wishlist ID {} and product ID {}",
                    wishlist.getId(), productId);
            throw new WishlistItemAlreadyExistsException(wishlist.getId(), productId);
        }

        log.info("[WISHLIST ITEM ADDED] Added product ID: {} to wishlist ID: {} for user ID: {}",
                productId, wishlist.getId(), userId);

        boolean inStock = isProductInStock(product);
        return WishlistMapper.toItemResponse(saved, inStock);
    }

    @Transactional
    public void removeProductFromWishlist(Long userId, Long productId) {
        WishlistEntity wishlist = getOrCreateWishlist(userId);

        WishlistItemEntity item = wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElseThrow(() -> new WishlistItemNotFoundException(wishlist.getId(), productId));

        wishlistItemRepository.delete(item);
        log.info("[WISHLIST ITEM REMOVED] Removed product ID: {} from wishlist ID: {} for user ID: {}",
                productId, wishlist.getId(), userId);
    }

    @Transactional
    public WishlistProductExistsResponse checkProductInWishlist(Long userId, Long productId) {
        WishlistEntity wishlist = getOrCreateWishlist(userId);
        boolean exists = wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId);

        return WishlistProductExistsResponse.builder()
                .productId(productId)
                .inWishlist(exists)
                .build();
    }

    @Transactional
    public void clearWishlist(Long userId) {
        WishlistEntity wishlist = getOrCreateWishlist(userId);
        wishlistItemRepository.deleteAllByWishlistId(wishlist.getId());
        log.info("[WISHLIST CLEARED] Cleared all items in wishlist ID: {} for user ID: {}", wishlist.getId(), userId);
    }

    private WishlistEntity getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserEntity user = resolveUser(userId);
                    WishlistEntity newWishlist = WishlistEntity.builder()
                            .user(user)
                            .name("My Wishlist")
                            .createdBy(user)
                            .updatedBy(user)
                            .build();
                    WishlistEntity saved = wishlistRepository.save(newWishlist);
                    log.info("[WISHLIST AUTO-CREATED] Created default wishlist ID: {} for user ID: {}", saved.getId(), userId);
                    return saved;
                });
    }

    private boolean isProductInStock(ProductEntity product) {
        if (product == null || product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }

        for (ProductVariantEntity variant : product.getVariants()) {
            if (variant.getStatus() == CatalogStatus.ACTIVE) {
                InventoryEntity inventory = inventoryRepository.findByVariantId(variant.getId()).orElse(null);
                if (inventory != null && inventory.getAvailableQuantity() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
}
