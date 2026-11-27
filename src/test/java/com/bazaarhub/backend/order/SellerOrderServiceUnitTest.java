package com.bazaarhub.backend.order;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.application.dto.request.CancelOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.UpdateOrderStatusRequest;
import com.bazaarhub.backend.order.application.dto.response.SellerOrderResponse;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.application.service.SellerOrderService;
import com.bazaarhub.backend.order.domain.exception.InvalidOrderOperationException;
import com.bazaarhub.backend.order.domain.exception.UnauthorizedOrderAccessException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerOrderServiceUnitTest {

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataOrderItemRepository orderItemRepository;

    @Mock
    private SpringDataOrderStatusHistoryRepository historyRepository;

    @Mock
    private SpringDataSellerRepository sellerRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private OrderStatusTransitionService orderStatusTransitionService;

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private SellerOrderService sellerOrderService;

    private UserEntity sellerUser;
    private SellerEntity seller;
    private OrderEntity order;
    private SellerOrderEntity sellerOrder;

    @BeforeEach
    void setUp() {
        sellerUser = UserEntity.builder().id(10L).fullName("Seller User").build();
        seller = SellerEntity.builder()
                .id(5L)
                .user(sellerUser)
                .businessName("Seller Store")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        order = OrderEntity.builder()
                .id(100L)
                .orderNumber("ORD-20261114-12345678")
                .status(OrderStatus.PENDING)
                .build();

        sellerOrder = SellerOrderEntity.builder()
                .id(200L)
                .order(order)
                .seller(seller)
                .subOrderNumber("SUB-20261114-12345678-S5")
                .status(SellerOrderStatus.PENDING)
                .subtotal(new BigDecimal("1000.00"))
                .totalAmount(new BigDecimal("1180.00"))
                .build();
    }

    @Test
    @DisplayName("Update seller order status throws UnauthorizedOrderAccessException if accessing other vendor's order")
    void testUpdateStatus_UnauthorizedSeller() {
        SellerEntity otherSeller = SellerEntity.builder().id(99L).build();
        sellerOrder.setSeller(otherSeller);

        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(userRepository.findById(sellerUser.getId())).thenReturn(Optional.of(sellerUser));
        when(sellerOrderRepository.findById(200L)).thenReturn(Optional.of(sellerOrder));

        assertThrows(UnauthorizedOrderAccessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(
                        sellerUser.getId(), 200L, UpdateOrderStatusRequest.builder().status("CONFIRMED").build()));
    }

    @Test
    @DisplayName("Update seller order status throws InvalidOrderOperationException for invalid status name")
    void testUpdateStatus_InvalidStatus() {
        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(userRepository.findById(sellerUser.getId())).thenReturn(Optional.of(sellerUser));
        when(sellerOrderRepository.findById(200L)).thenReturn(Optional.of(sellerOrder));

        assertThrows(InvalidOrderOperationException.class, () ->
                sellerOrderService.updateSellerOrderStatus(
                        sellerUser.getId(), 200L, UpdateOrderStatusRequest.builder().status("NON_EXISTENT_STATUS").build()));
    }

    @Test
    @DisplayName("Cancel seller order fails if sub-order is already SHIPPED")
    void testCancelSellerOrder_AlreadyShipped() {
        sellerOrder.setStatus(SellerOrderStatus.SHIPPED);

        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(userRepository.findById(sellerUser.getId())).thenReturn(Optional.of(sellerUser));
        when(sellerOrderRepository.findById(200L)).thenReturn(Optional.of(sellerOrder));

        assertThrows(InvalidOrderOperationException.class, () ->
                sellerOrderService.cancelSellerOrder(sellerUser.getId(), 200L, new CancelOrderRequest()));
    }
}
