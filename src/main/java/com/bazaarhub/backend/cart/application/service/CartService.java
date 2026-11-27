package com.bazaarhub.backend.cart.application.service;

import com.bazaarhub.backend.cart.application.dto.request.AddToCartRequest;
import com.bazaarhub.backend.cart.application.dto.request.UpdateCartItemQuantityRequest;
import com.bazaarhub.backend.cart.application.dto.response.CartResponse;
import com.bazaarhub.backend.cart.application.dto.response.CartSummaryResponse;
import com.bazaarhub.backend.cart.application.mapper.CartMapper;
import com.bazaarhub.backend.cart.domain.exception.CartItemNotFoundException;
import com.bazaarhub.backend.cart.domain.exception.CartNotFoundException;
import com.bazaarhub.backend.cart.domain.exception.InvalidCartOperationException;
import com.bazaarhub.backend.cart.domain.exception.UnauthorizedCartAccessException;
import com.bazaarhub.backend.cart.domain.model.CartStatus;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartItemEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartItemRepository;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartRepository;
import com.bazaarhub.backend.catalog.domain.exception.ProductVariantNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core application service managing the customer shopping cart lifecycle:
 * Add to cart, quantity update, item removal, multi-vendor cart grouping,
 * inventory validation, and server-side pricing.
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final SpringDataCartRepository cartRepository;
    private final SpringDataCartItemRepository cartItemRepository;
    private final SpringDataProductVariantRepository variantRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataInventoryRepository inventoryRepository;

    public CartService(SpringDataCartRepository cartRepository,
                       SpringDataCartItemRepository cartItemRepository,
                       SpringDataProductVariantRepository variantRepository,
                       SpringDataUserRepository userRepository,
                       SpringDataInventoryRepository inventoryRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Retrieves the authenticated customer's cart, creating an active cart if none exists.
     */
    @Transactional
    public CartResponse getCart(Long userId) {
        CartEntity cart = getOrCreateCart(userId);
        Map<Long, Integer> stockMap = resolveVariantStockMap(cart.getItems());
        return CartMapper.toCartResponse(cart, stockMap);
    }

    /**
     * Adds a product variant to the customer's cart with inventory and visibility validation.
     */
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        if (request == null || request.getVariantId() == null) {
            throw new InvalidCartOperationException("Variant ID is required");
        }
        int quantityToAdd = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantityToAdd < 1) {
            throw new InvalidCartOperationException("Quantity must be at least 1");
        }

        UserEntity user = resolveUser(userId);

        // 1. Fetch variant and validate catalog integrity
        ProductVariantEntity variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(request.getVariantId()));

        ProductEntity product = variant.getProduct();
        if (product == null) {
            throw new InvalidCartOperationException("Product variant has no associated product");
        }

        if (request.getProductId() != null && !product.getId().equals(request.getProductId())) {
            throw new InvalidCartOperationException("Selected variant does not belong to specified product ID: " + request.getProductId());
        }

        // 2. Validate product visibility and approval status
        if (product.getStatus() != ProductStatus.ACTIVE || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            throw new InvalidCartOperationException("Product is not active or approved for retail sale");
        }

        // 3. Validate seller approval status
        if (product.getSeller() == null || product.getSeller().getApprovalStatus() != SellerApprovalStatus.APPROVED) {
            throw new InvalidCartOperationException("Merchant store is not currently eligible for customer orders");
        }

        // 4. Validate variant active status
        if (variant.getStatus() != CatalogStatus.ACTIVE) {
            throw new InvalidCartOperationException("Product variant is inactive");
        }

        // 5. Validate available inventory
        int availableStock = getAvailableStock(variant.getId());
        if (availableStock <= 0) {
            throw new InsufficientStockException("Variant SKU '" + variant.getSku() + "' is currently out of stock");
        }

        CartEntity cart = getOrCreateCart(userId);

        // 6. Check if variant already exists in cart -> update quantity without duplicate row
        Optional<CartItemEntity> existingItemOpt = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());
        if (existingItemOpt.isPresent()) {
            CartItemEntity existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantityToAdd;
            if (newQuantity > availableStock) {
                throw new InsufficientStockException("Requested quantity (" + newQuantity + ") exceeds available stock (" + availableStock + ")");
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setUpdatedBy(user);
            cartItemRepository.save(existingItem);
            log.info("[CART ITEM UPDATED] Incremented variant ID {} quantity to {} in cart ID {}",
                    variant.getId(), newQuantity, cart.getId());
        } else {
            if (quantityToAdd > availableStock) {
                throw new InsufficientStockException("Requested quantity (" + quantityToAdd + ") exceeds available stock (" + availableStock + ")");
            }
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(quantityToAdd)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
            log.info("[CART ITEM ADDED] Added variant ID {} quantity {} to cart ID {}",
                    variant.getId(), quantityToAdd, cart.getId());
        }

        cart.setUpdatedAt(Instant.now());
        cart.setUpdatedBy(user);
        cartRepository.save(cart);

        Map<Long, Integer> stockMap = resolveVariantStockMap(cart.getItems());
        return CartMapper.toCartResponse(cart, stockMap);
    }

    /**
     * Updates the quantity of an existing cart item.
     */
    @Transactional
    public CartResponse updateCartItemQuantity(Long userId, Long itemId, UpdateCartItemQuantityRequest request) {
        if (itemId == null) {
            throw new InvalidCartOperationException("Cart item ID cannot be null");
        }
        if (request == null || request.getQuantity() == null || request.getQuantity() < 1) {
            throw new InvalidCartOperationException("Updated quantity must be at least 1");
        }

        CartEntity cart = getOrCreateCart(userId);
        UserEntity user = resolveUser(userId);

        // Verify item belongs to customer's cart
        CartItemEntity item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new CartItemNotFoundException("Cart item ID " + itemId + " not found in customer cart"));

        ProductVariantEntity variant = item.getVariant();
        if (variant == null || variant.getStatus() != CatalogStatus.ACTIVE) {
            throw new InvalidCartOperationException("Product variant is no longer active");
        }

        ProductEntity product = variant.getProduct();
        if (product == null || product.getStatus() != ProductStatus.ACTIVE || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            throw new InvalidCartOperationException("Product is no longer active or approved for sale");
        }

        if (product.getSeller() == null || product.getSeller().getApprovalStatus() != SellerApprovalStatus.APPROVED) {
            throw new InvalidCartOperationException("Merchant store is no longer active");
        }

        int availableStock = getAvailableStock(variant.getId());
        if (request.getQuantity() > availableStock) {
            throw new InsufficientStockException("Requested quantity (" + request.getQuantity() + ") exceeds available stock (" + availableStock + ")");
        }

        item.setQuantity(request.getQuantity());
        item.setUpdatedBy(user);
        cartItemRepository.save(item);

        cart.setUpdatedAt(Instant.now());
        cart.setUpdatedBy(user);
        cartRepository.save(cart);

        log.info("[CART ITEM UPDATED] Updated cart item ID {} to quantity {} for user ID {}",
                itemId, request.getQuantity(), userId);

        Map<Long, Integer> stockMap = resolveVariantStockMap(cart.getItems());
        return CartMapper.toCartResponse(cart, stockMap);
    }

    /**
     * Removes a single cart item from the customer's cart, enforcing strict ownership check.
     */
    @Transactional
    public CartResponse removeCartItem(Long userId, Long itemId) {
        if (itemId == null) {
            throw new InvalidCartOperationException("Cart item ID cannot be null");
        }

        CartEntity cart = getOrCreateCart(userId);

        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item ID " + itemId + " was not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new UnauthorizedCartAccessException("Cannot remove cart item belonging to another customer");
        }

        cartItemRepository.delete(item);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        log.info("[CART ITEM REMOVED] Removed cart item ID {} from cart ID {}", itemId, cart.getId());

        Map<Long, Integer> stockMap = resolveVariantStockMap(cart.getItems());
        return CartMapper.toCartResponse(cart, stockMap);
    }

    /**
     * Clears all items from the customer's active cart.
     */
    @Transactional
    public CartResponse clearCart(Long userId) {
        CartEntity cart = getOrCreateCart(userId);

        cart.getItems().clear();

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        log.info("[CART CLEARED] Cleared all items in cart ID {} for user ID {}", cart.getId(), userId);

        return CartMapper.toCartResponse(cart, Collections.emptyMap());
    }

    /**
     * Returns high-level cart summary totals without full item metadata.
     */
    @Transactional(readOnly = true)
    public CartSummaryResponse getCartSummary(Long userId) {
        CartResponse cartResponse = getCart(userId);
        return CartMapper.toSummaryResponse(cartResponse);
    }

    /**
     * Resolves or auto-creates an active cart for the customer.
     */
    public CartEntity getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Optional<CartEntity> existing = cartRepository.findByUserId(userId);
                    if (existing.isPresent()) {
                        CartEntity cart = existing.get();
                        cart.setStatus(CartStatus.ACTIVE);
                        cart.getItems().clear();
                        cart.setUpdatedAt(Instant.now());
                        return cartRepository.save(cart);
                    }
                    UserEntity user = resolveUser(userId);
                    CartEntity newCart = CartEntity.builder()
                            .user(user)
                            .status(CartStatus.ACTIVE)
                            .createdBy(user)
                            .updatedBy(user)
                            .build();
                    CartEntity saved = cartRepository.save(newCart);
                    log.info("[CART AUTO-CREATED] Created new active cart ID {} for user ID {}", saved.getId(), userId);
                    return saved;
                });
    }

    public int getAvailableStock(Long variantId) {
        if (variantId == null) {
            return 0;
        }
        return inventoryRepository.findByVariantId(variantId)
                .map(InventoryEntity::getAvailableQuantity)
                .orElse(0);
    }

    public Map<Long, Integer> resolveVariantStockMap(List<CartItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> variantIds = items.stream()
                .filter(i -> i.getVariant() != null)
                .map(i -> i.getVariant().getId())
                .distinct()
                .toList();

        if (variantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<InventoryEntity> inventories = inventoryRepository.findAll((root, query, cb) ->
                root.get("variant").get("id").in(variantIds));

        Map<Long, Integer> stockMap = new HashMap<>();
        for (InventoryEntity inv : inventories) {
            if (inv.getVariant() != null) {
                stockMap.put(inv.getVariant().getId(), inv.getAvailableQuantity());
            }
        }
        return stockMap;
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
}
