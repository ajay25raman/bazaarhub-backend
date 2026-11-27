package com.bazaarhub.backend.order;

import com.bazaarhub.backend.cart.application.service.CartService;
import com.bazaarhub.backend.cart.domain.model.CartStatus;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartItemEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartRepository;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.customer.domain.exception.UnauthorizedAddressAccessException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.StockReserveRequest;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.application.dto.request.CancelOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.CreateOrderRequest;
import com.bazaarhub.backend.order.application.dto.response.OrderResponse;
import com.bazaarhub.backend.order.application.service.OrderService;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.exception.InvalidOrderOperationException;
import com.bazaarhub.backend.order.domain.exception.OrderNotFoundException;
import com.bazaarhub.backend.order.domain.exception.UnauthorizedOrderAccessException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataOrderItemRepository orderItemRepository;

    @Mock
    private SpringDataOrderStatusHistoryRepository historyRepository;

    @Mock
    private CartService cartService;

    @Mock
    private SpringDataCartRepository cartRepository;

    @Mock
    private SpringDataAddressRepository addressRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderStatusTransitionService orderStatusTransitionService;

    @InjectMocks
    private OrderService orderService;

    private UserEntity customer;
    private AddressEntity address;
    private SellerEntity seller;
    private ProductEntity product;
    private ProductVariantEntity variant;
    private CartEntity cart;
    private CartItemEntity cartItem;

    @BeforeEach
    void setUp() {
        customer = UserEntity.builder()
                .id(1L)
                .fullName("Customer One")
                .email("customer1@test.com")
                .build();

        address = AddressEntity.builder()
                .id(10L)
                .user(customer)
                .recipientName("Customer One")
                .addressLine1("123 Main St")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .phone("9876543210")
                .build();

        seller = SellerEntity.builder()
                .id(5L)
                .businessName("Seller Alpha")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        product = ProductEntity.builder()
                .id(20L)
                .name("Test Product")
                .seller(seller)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build();

        variant = ProductVariantEntity.builder()
                .id(100L)
                .product(product)
                .sku("SKU-100")
                .variantName("Default Variant")
                .sellingPrice(new BigDecimal("999.00"))
                .mrp(new BigDecimal("1299.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build();

        cartItem = CartItemEntity.builder()
                .id(1L)
                .variant(variant)
                .quantity(2)
                .build();

        cart = CartEntity.builder()
                .id(50L)
                .user(customer)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();
    }

    @Test
    @DisplayName("Create order fails if customer does not exist")
    void testCreateOrder_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                orderService.createOrder(999L, CreateOrderRequest.builder().addressId(10L).build()));
    }

    @Test
    @DisplayName("Create order fails if cart is empty")
    void testCreateOrder_EmptyCart() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        cart.getItems().clear();
        when(cartService.getOrCreateCart(customer.getId())).thenReturn(cart);

        assertThrows(InvalidOrderOperationException.class, () ->
                orderService.createOrder(customer.getId(), CreateOrderRequest.builder().addressId(10L).build()));
    }

    @Test
    @DisplayName("Create order fails if address belongs to another customer")
    void testCreateOrder_AddressMismatch() {
        UserEntity otherUser = UserEntity.builder().id(2L).build();
        address.setUser(otherUser);

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(cartService.getOrCreateCart(customer.getId())).thenReturn(cart);
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));

        assertThrows(UnauthorizedAddressAccessException.class, () ->
                orderService.createOrder(customer.getId(), CreateOrderRequest.builder().addressId(10L).build()));
    }

    @Test
    @DisplayName("Create order fails if insufficient warehouse stock")
    void testCreateOrder_InsufficientStock() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(cartService.getOrCreateCart(customer.getId())).thenReturn(cart);
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(cartService.getAvailableStock(variant.getId())).thenReturn(1); // Requested 2, available 1

        assertThrows(InsufficientStockException.class, () ->
                orderService.createOrder(customer.getId(), CreateOrderRequest.builder().addressId(10L).build()));
    }

    @Test
    @DisplayName("Cancel order throws UnauthorizedOrderAccessException if accessing another user's order")
    void testCancelOrder_Unauthorized() {
        UserEntity otherUser = UserEntity.builder().id(2L).build();
        OrderEntity order = OrderEntity.builder().id(77L).user(otherUser).build();

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedOrderAccessException.class, () ->
                orderService.cancelCustomerOrder(customer.getId(), 77L, new CancelOrderRequest()));
    }

    @Test
    @DisplayName("Cancel order throws InvalidOrderOperationException if order is in DELIVERED status")
    void testCancelOrder_InvalidState() {
        OrderEntity order = OrderEntity.builder()
                .id(77L)
                .user(customer)
                .status(OrderStatus.DELIVERED)
                .build();

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderOperationException.class, () ->
                orderService.cancelCustomerOrder(customer.getId(), 77L, new CancelOrderRequest()));
    }
}
