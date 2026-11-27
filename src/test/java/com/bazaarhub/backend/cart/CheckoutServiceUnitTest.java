package com.bazaarhub.backend.cart;

import com.bazaarhub.backend.cart.application.dto.request.CheckoutAddressRequest;
import com.bazaarhub.backend.cart.application.dto.request.CheckoutValidateRequest;
import com.bazaarhub.backend.cart.application.dto.response.CartItemResponse;
import com.bazaarhub.backend.cart.application.dto.response.CartResponse;
import com.bazaarhub.backend.cart.application.dto.response.CheckoutSummaryResponse;
import com.bazaarhub.backend.cart.application.dto.response.CheckoutValidationResponse;
import com.bazaarhub.backend.cart.application.dto.response.SellerCartGroupDto;
import com.bazaarhub.backend.cart.application.service.CartService;
import com.bazaarhub.backend.cart.application.service.CheckoutService;
import com.bazaarhub.backend.cart.domain.exception.EmptyCartException;
import com.bazaarhub.backend.cart.domain.model.CartStatus;
import com.bazaarhub.backend.customer.domain.exception.AddressNotFoundException;
import com.bazaarhub.backend.customer.domain.exception.UnauthorizedAddressAccessException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceUnitTest {

    @Mock
    private CartService cartService;

    @Mock
    private SpringDataAddressRepository addressRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    private UserEntity user;
    private AddressEntity address;
    private CartResponse validCartResponse;
    private CartItemResponse validCartItem;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .email("customer@bazaarhub.com")
                .fullName("John Customer")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        address = AddressEntity.builder()
                .id(50L)
                .user(user)
                .recipientName("John Customer")
                .phone("+919876543210")
                .addressLine1("123 Tech Park, Whitefield")
                .city("Bangalore")
                .state("Karnataka")
                .country("India")
                .postalCode("560066")
                .addressType("HOME")
                .isDefault(true)
                .build();

        validCartItem = CartItemResponse.builder()
                .itemId(10L)
                .variantId(30L)
                .productId(20L)
                .productName("Wireless Headphones")
                .variantName("Black")
                .sku("WNC-BLK-01")
                .unitPrice(new BigDecimal("1999.00"))
                .quantity(2)
                .totalPrice(new BigDecimal("3998.00"))
                .availableStock(10)
                .inStock(true)
                .purchasable(true)
                .sellerId(100L)
                .sellerBusinessName("Tech World")
                .build();

        SellerCartGroupDto sellerGroup = SellerCartGroupDto.builder()
                .sellerId(100L)
                .businessName("Tech World")
                .itemCount(2)
                .subtotal(new BigDecimal("3998.00"))
                .items(List.of(validCartItem))
                .build();

        validCartResponse = CartResponse.builder()
                .cartId(200L)
                .status(CartStatus.ACTIVE)
                .totalItems(2)
                .totalUniqueItems(1)
                .subtotal(new BigDecimal("3998.00"))
                .taxAmount(new BigDecimal("719.64"))
                .deliveryFee(new BigDecimal("0.00"))
                .grandTotal(new BigDecimal("4717.64"))
                .items(List.of(validCartItem))
                .sellers(List.of(sellerGroup))
                .build();
    }

    @Test
    @DisplayName("getCheckoutSummary - empty cart returns cannot proceed and empty cart status")
    void testGetCheckoutSummary_EmptyCart() {
        CartResponse emptyCart = CartResponse.builder()
                .cartId(200L)
                .status(CartStatus.ACTIVE)
                .totalItems(0)
                .totalUniqueItems(0)
                .items(new ArrayList<>())
                .build();

        when(cartService.getCart(1L)).thenReturn(emptyCart);

        CheckoutSummaryResponse summary = checkoutService.getCheckoutSummary(1L, null);

        assertNotNull(summary);
        assertFalse(summary.isCanProceedToCheckout());
        assertEquals("EMPTY_CART", summary.getCheckoutStatus());
        assertTrue(summary.getValidationErrors().stream().anyMatch(e -> e.contains("Cart is empty")));
    }

    @Test
    @DisplayName("getCheckoutSummary - valid cart with valid specified address proceeds to checkout READY")
    void testGetCheckoutSummary_ValidCartAndAddress() {
        when(cartService.getCart(1L)).thenReturn(validCartResponse);
        when(addressRepository.findById(50L)).thenReturn(Optional.of(address));

        CheckoutSummaryResponse summary = checkoutService.getCheckoutSummary(1L, 50L);

        assertNotNull(summary);
        assertTrue(summary.isCanProceedToCheckout());
        assertEquals("READY", summary.getCheckoutStatus());
        assertNotNull(summary.getSelectedAddress());
        assertEquals(50L, summary.getSelectedAddress().getId());
        assertEquals(new BigDecimal("4717.64"), summary.getGrandTotal());
        assertTrue(summary.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("getCheckoutSummary - valid cart with fallback default address proceeds to checkout READY")
    void testGetCheckoutSummary_FallbackDefaultAddress() {
        when(cartService.getCart(1L)).thenReturn(validCartResponse);
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(address));

        CheckoutSummaryResponse summary = checkoutService.getCheckoutSummary(1L, null);

        assertNotNull(summary);
        assertTrue(summary.isCanProceedToCheckout());
        assertEquals("READY", summary.getCheckoutStatus());
        assertNotNull(summary.getSelectedAddress());
        assertEquals(50L, summary.getSelectedAddress().getId());
    }

    @Test
    @DisplayName("getCheckoutSummary - valid cart without any address requires action")
    void testGetCheckoutSummary_NoAddressRequiresAction() {
        when(cartService.getCart(1L)).thenReturn(validCartResponse);
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.empty());

        CheckoutSummaryResponse summary = checkoutService.getCheckoutSummary(1L, null);

        assertNotNull(summary);
        assertFalse(summary.isCanProceedToCheckout());
        assertEquals("ACTION_REQUIRED", summary.getCheckoutStatus());
        assertNull(summary.getSelectedAddress());
        assertTrue(summary.getWarnings().stream().anyMatch(w -> w.contains("No delivery address selected")));
    }

    @Test
    @DisplayName("getCheckoutSummary - item out of stock adds validation error and blocks checkout")
    void testGetCheckoutSummary_OutOfStockItem() {
        CartItemResponse outOfStockItem = CartItemResponse.builder()
                .itemId(10L)
                .productName("Wireless Headphones")
                .variantName("Black")
                .quantity(5)
                .availableStock(2)
                .inStock(false)
                .purchasable(true)
                .build();

        CartResponse cartWithOutOfStock = CartResponse.builder()
                .cartId(200L)
                .status(CartStatus.ACTIVE)
                .totalItems(1)
                .items(List.of(outOfStockItem))
                .build();

        when(cartService.getCart(1L)).thenReturn(cartWithOutOfStock);
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(address));

        CheckoutSummaryResponse summary = checkoutService.getCheckoutSummary(1L, null);

        assertNotNull(summary);
        assertFalse(summary.isCanProceedToCheckout());
        assertEquals("ACTION_REQUIRED", summary.getCheckoutStatus());
        assertTrue(summary.getValidationErrors().stream().anyMatch(e -> e.contains("insufficient stock")));
    }

    @Test
    @DisplayName("getCheckoutSummary - throws UnauthorizedAddressAccessException when address belongs to another customer")
    void testGetCheckoutSummary_AnotherCustomersAddress() {
        UserEntity anotherUser = UserEntity.builder().id(99L).build();
        AddressEntity anotherAddress = AddressEntity.builder().id(88L).user(anotherUser).build();

        when(cartService.getCart(1L)).thenReturn(validCartResponse);
        when(addressRepository.findById(88L)).thenReturn(Optional.of(anotherAddress));

        assertThrows(UnauthorizedAddressAccessException.class,
                () -> checkoutService.getCheckoutSummary(1L, 88L));
    }

    @Test
    @DisplayName("getCheckoutSummary - throws AddressNotFoundException when address ID does not exist")
    void testGetCheckoutSummary_AddressNotFound() {
        when(cartService.getCart(1L)).thenReturn(validCartResponse);
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class,
                () -> checkoutService.getCheckoutSummary(1L, 999L));
    }

    @Test
    @DisplayName("validateCheckout - throws EmptyCartException when cart is empty")
    void testValidateCheckout_EmptyCart() {
        CartResponse emptyCart = CartResponse.builder()
                .cartId(200L)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build();

        when(cartService.getCart(1L)).thenReturn(emptyCart);

        assertThrows(EmptyCartException.class,
                () -> checkoutService.validateCheckout(1L, new CheckoutValidateRequest(50L)));
    }

    @Test
    @DisplayName("validateCheckout - returns valid=true when cart items and address are valid")
    void testValidateCheckout_Valid() {
        when(cartService.getCart(1L)).thenReturn(validCartResponse);
        when(addressRepository.findById(50L)).thenReturn(Optional.of(address));

        CheckoutValidationResponse response = checkoutService.validateCheckout(1L, new CheckoutValidateRequest(50L));

        assertNotNull(response);
        assertTrue(response.isValid());
        assertTrue(response.getErrors().isEmpty());
        assertEquals(200L, response.getCartId());
        assertEquals(50L, response.getAddressId());
    }

    @Test
    @DisplayName("validateCheckout - returns valid=false when item is unpurchasable")
    void testValidateCheckout_UnpurchasableItem() {
        CartItemResponse unpurchasableItem = CartItemResponse.builder()
                .itemId(10L)
                .productName("Inactive Phone")
                .variantName("Grey")
                .quantity(1)
                .availableStock(10)
                .inStock(true)
                .purchasable(false)
                .build();

        CartResponse cart = CartResponse.builder()
                .cartId(200L)
                .status(CartStatus.ACTIVE)
                .items(List.of(unpurchasableItem))
                .build();

        when(cartService.getCart(1L)).thenReturn(cart);
        when(addressRepository.findById(50L)).thenReturn(Optional.of(address));

        CheckoutValidationResponse response = checkoutService.validateCheckout(1L, new CheckoutValidateRequest(50L));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.getErrors().stream().anyMatch(e -> e.contains("not eligible for checkout")));
    }

    @Test
    @DisplayName("selectCheckoutAddress - successfully verifies and selects delivery address")
    void testSelectCheckoutAddress_Success() {
        when(addressRepository.findById(50L)).thenReturn(Optional.of(address));
        when(cartService.getCart(1L)).thenReturn(validCartResponse);

        CheckoutSummaryResponse summary = checkoutService.selectCheckoutAddress(1L, new CheckoutAddressRequest(50L));

        assertNotNull(summary);
        assertTrue(summary.isCanProceedToCheckout());
        assertEquals(50L, summary.getSelectedAddress().getId());
    }
}
