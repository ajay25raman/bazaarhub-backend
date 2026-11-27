package com.bazaarhub.backend.cart;

import com.bazaarhub.backend.cart.application.dto.request.AddToCartRequest;
import com.bazaarhub.backend.cart.application.dto.request.UpdateCartItemQuantityRequest;
import com.bazaarhub.backend.cart.application.dto.response.CartResponse;
import com.bazaarhub.backend.cart.application.dto.response.CartSummaryResponse;
import com.bazaarhub.backend.cart.application.service.CartService;
import com.bazaarhub.backend.cart.domain.exception.CartItemNotFoundException;
import com.bazaarhub.backend.cart.domain.exception.InvalidCartOperationException;
import com.bazaarhub.backend.cart.domain.exception.UnauthorizedCartAccessException;
import com.bazaarhub.backend.cart.domain.model.CartStatus;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartItemEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartItemRepository;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartRepository;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceUnitTest {

    @Mock
    private SpringDataCartRepository cartRepository;

    @Mock
    private SpringDataCartItemRepository cartItemRepository;

    @Mock
    private SpringDataProductVariantRepository variantRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    @InjectMocks
    private CartService cartService;

    private UserEntity user;
    private CartEntity cart;
    private SellerEntity seller;
    private ProductEntity product;
    private ProductVariantEntity variant;
    private InventoryEntity inventory;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .email("customer@bazaarhub.com")
                .fullName("John Customer")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        cart = CartEntity.builder()
                .id(100L)
                .user(user)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build();

        seller = SellerEntity.builder()
                .id(10L)
                .businessName("Tech World")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        product = ProductEntity.builder()
                .id(20L)
                .name("Wireless Noise Canceling Headphones")
                .seller(seller)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build();

        variant = ProductVariantEntity.builder()
                .id(30L)
                .product(product)
                .sku("WNC-BLK-01")
                .variantName("Matte Black")
                .sellingPrice(new BigDecimal("1999.00"))
                .mrp(new BigDecimal("2499.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build();

        inventory = InventoryEntity.builder()
                .id(40L)
                .variant(variant)
                .totalQuantity(50)
                .reservedQuantity(5)
                .safetyStock(2)
                .build();
    }

    @Test
    @DisplayName("getCart - returns existing cart mapped with real-time stock")
    void testGetCart_ExistingCart() {
        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(1L);

        assertNotNull(response);
        assertEquals(100L, response.getCartId());
        assertEquals(CartStatus.ACTIVE, response.getStatus());
        assertEquals(0, response.getTotalItems());
        assertEquals(BigDecimal.ZERO.setScale(2), response.getSubtotal());
    }

    @Test
    @DisplayName("getCart - auto creates new cart if none exists")
    void testGetCart_AutoCreatesCart() {
        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> {
            CartEntity c = invocation.getArgument(0);
            c.setId(101L);
            return c;
        });

        CartResponse response = cartService.getCart(1L);

        assertNotNull(response);
        assertEquals(101L, response.getCartId());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("addToCart - successfully adds new item when stock is sufficient")
    void testAddToCart_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(variantRepository.findById(30L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantId(30L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 30L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);
        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(List.of(inventory));

        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(30L)
                .quantity(2)
                .build();

        CartResponse response = cartService.addToCart(1L, request);

        assertNotNull(response);
        assertEquals(2, response.getTotalItems());
        assertEquals(1, response.getTotalUniqueItems());
        assertEquals(new BigDecimal("3998.00"), response.getSubtotal());
        verify(cartItemRepository, times(1)).save(any(CartItemEntity.class));
    }

    @Test
    @DisplayName("addToCart - increments quantity when variant already in cart")
    void testAddToCart_IncrementsExistingItem() {
        CartItemEntity existingItem = CartItemEntity.builder()
                .id(200L)
                .cart(cart)
                .variant(variant)
                .quantity(2)
                .build();
        cart.getItems().add(existingItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(variantRepository.findById(30L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantId(30L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 30L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItemEntity.class))).thenReturn(existingItem);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);
        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(List.of(inventory));

        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(30L)
                .quantity(3)
                .build();

        CartResponse response = cartService.addToCart(1L, request);

        assertNotNull(response);
        assertEquals(5, existingItem.getQuantity());
        assertEquals(5, response.getTotalItems());
    }

    @Test
    @DisplayName("addToCart - throws InsufficientStockException when quantity exceeds available stock")
    void testAddToCart_ExceedsStock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(variantRepository.findById(30L)).thenReturn(Optional.of(variant));
        // Available = 45
        when(inventoryRepository.findByVariantId(30L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(30L)
                .quantity(50) // exceeds 45
                .build();

        assertThrows(InsufficientStockException.class, () -> cartService.addToCart(1L, request));
    }

    @Test
    @DisplayName("addToCart - throws InvalidCartOperationException when product is inactive")
    void testAddToCart_InactiveProduct() {
        product.setStatus(ProductStatus.INACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(variantRepository.findById(30L)).thenReturn(Optional.of(variant));

        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(30L)
                .quantity(1)
                .build();

        assertThrows(InvalidCartOperationException.class, () -> cartService.addToCart(1L, request));
    }

    @Test
    @DisplayName("addToCart - throws InvalidCartOperationException when variant is inactive")
    void testAddToCart_InactiveVariant() {
        variant.setStatus(CatalogStatus.INACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(variantRepository.findById(30L)).thenReturn(Optional.of(variant));

        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(30L)
                .quantity(1)
                .build();

        assertThrows(InvalidCartOperationException.class, () -> cartService.addToCart(1L, request));
    }

    @Test
    @DisplayName("addToCart - throws InvalidCartOperationException when merchant is not approved")
    void testAddToCart_UnapprovedSeller() {
        seller.setApprovalStatus(SellerApprovalStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(variantRepository.findById(30L)).thenReturn(Optional.of(variant));

        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(30L)
                .quantity(1)
                .build();

        assertThrows(InvalidCartOperationException.class, () -> cartService.addToCart(1L, request));
    }

    @Test
    @DisplayName("updateCartItemQuantity - successfully updates quantity within available stock")
    void testUpdateCartItemQuantity_Success() {
        CartItemEntity item = CartItemEntity.builder()
                .id(200L)
                .cart(cart)
                .variant(variant)
                .quantity(1)
                .build();
        cart.getItems().add(item);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByIdAndCartId(200L, 100L)).thenReturn(Optional.of(item));
        when(inventoryRepository.findByVariantId(30L)).thenReturn(Optional.of(inventory));
        when(cartItemRepository.save(any(CartItemEntity.class))).thenReturn(item);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);
        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(List.of(inventory));

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(4);

        CartResponse response = cartService.updateCartItemQuantity(1L, 200L, request);

        assertNotNull(response);
        assertEquals(4, item.getQuantity());
        assertEquals(4, response.getTotalItems());
        assertEquals(1, response.getTotalUniqueItems());
        assertEquals(new BigDecimal("7996.00"), response.getSubtotal());
    }

    @Test
    @DisplayName("updateCartItemQuantity - throws InsufficientStockException when updated quantity exceeds stock")
    void testUpdateCartItemQuantity_ExceedsStock() {
        CartItemEntity item = CartItemEntity.builder()
                .id(200L)
                .cart(cart)
                .variant(variant)
                .quantity(1)
                .build();
        cart.getItems().add(item);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByIdAndCartId(200L, 100L)).thenReturn(Optional.of(item));
        when(inventoryRepository.findByVariantId(30L)).thenReturn(Optional.of(inventory)); // available 45

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(50);

        assertThrows(InsufficientStockException.class, () -> cartService.updateCartItemQuantity(1L, 200L, request));
    }

    @Test
    @DisplayName("updateCartItemQuantity - throws CartItemNotFoundException when item not in customer's cart")
    void testUpdateCartItemQuantity_ItemNotFound() {
        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByIdAndCartId(999L, 100L)).thenReturn(Optional.empty());

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(2);

        assertThrows(CartItemNotFoundException.class, () -> cartService.updateCartItemQuantity(1L, 999L, request));
    }

    @Test
    @DisplayName("removeCartItem - successfully removes item belonging to customer cart")
    void testRemoveCartItem_Success() {
        CartItemEntity item = CartItemEntity.builder()
                .id(200L)
                .cart(cart)
                .variant(variant)
                .quantity(2)
                .build();
        cart.getItems().add(item);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(200L)).thenReturn(Optional.of(item));

        CartResponse response = cartService.removeCartItem(1L, 200L);

        assertNotNull(response);
        assertEquals(0, response.getTotalItems());
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("removeCartItem - throws UnauthorizedCartAccessException when item belongs to another cart")
    void testRemoveCartItem_AnotherCustomersItem() {
        CartEntity anotherCart = CartEntity.builder()
                .id(999L)
                .user(UserEntity.builder().id(2L).build())
                .status(CartStatus.ACTIVE)
                .build();

        CartItemEntity item = CartItemEntity.builder()
                .id(200L)
                .cart(anotherCart)
                .variant(variant)
                .quantity(1)
                .build();

        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(200L)).thenReturn(Optional.of(item));

        assertThrows(UnauthorizedCartAccessException.class, () -> cartService.removeCartItem(1L, 200L));
    }

    @Test
    @DisplayName("clearCart - deletes all items in cart")
    void testClearCart_Success() {
        CartItemEntity item = CartItemEntity.builder()
                .id(200L)
                .cart(cart)
                .variant(variant)
                .quantity(2)
                .build();
        cart.getItems().add(item);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(1L);

        assertNotNull(response);
        assertEquals(0, response.getTotalItems());
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    @DisplayName("getCartSummary - returns high-level cart totals")
    void testGetCartSummary_Success() {
        CartItemEntity item = CartItemEntity.builder()
                .id(200L)
                .cart(cart)
                .variant(variant)
                .quantity(3)
                .build();
        cart.getItems().add(item);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(List.of(inventory));

        CartSummaryResponse summary = cartService.getCartSummary(1L);

        assertNotNull(summary);
        assertEquals(3, summary.getTotalItems());
        assertEquals(1, summary.getTotalUniqueItems());
        assertEquals(new BigDecimal("5997.00"), summary.getSubtotal());
        assertNotNull(summary.getSellers());
        assertEquals(1, summary.getSellers().size());
    }
}
