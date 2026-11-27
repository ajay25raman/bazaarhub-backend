package com.bazaarhub.backend.order;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.exception.InvalidOrderStatusTransitionException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusTransitionServiceUnitTest {

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataOrderStatusHistoryRepository historyRepository;

    @InjectMocks
    private OrderStatusTransitionService transitionService;

    private UserEntity user;
    private OrderEntity order;
    private SellerOrderEntity sellerOrder;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(1L).fullName("Test User").build();
        order = OrderEntity.builder()
                .id(10L)
                .orderNumber("ORD-20261114-11111111")
                .status(OrderStatus.PENDING)
                .build();

        sellerOrder = SellerOrderEntity.builder()
                .id(20L)
                .order(order)
                .subOrderNumber("SUB-20261114-11111111-S1")
                .status(SellerOrderStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Valid parent order transition: PENDING -> CONFIRMED")
    void testTransitionOrderStatus_Success() {
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        OrderEntity result = transitionService.transitionOrderStatus(order, OrderStatus.CONFIRMED, "Order confirmed", user);

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(historyRepository).save(any(OrderStatusHistoryEntity.class));
    }

    @Test
    @DisplayName("Invalid parent order transition throws InvalidOrderStatusTransitionException: PENDING -> DELIVERED")
    void testTransitionOrderStatus_InvalidTransition() {
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                transitionService.transitionOrderStatus(order, OrderStatus.DELIVERED, "Direct to delivered", user));
    }

    @Test
    @DisplayName("Valid seller sub-order transition: PENDING -> CONFIRMED")
    void testTransitionSellerOrderStatus_Success() {
        when(sellerOrderRepository.save(any(SellerOrderEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(sellerOrderRepository.findByOrderId(order.getId())).thenReturn(List.of(sellerOrder));

        SellerOrderEntity result = transitionService.transitionSellerOrderStatus(
                sellerOrder, SellerOrderStatus.CONFIRMED, "Vendor confirmed sub-order", user);

        assertEquals(SellerOrderStatus.CONFIRMED, result.getStatus());
        verify(historyRepository, atLeastOnce()).save(any(OrderStatusHistoryEntity.class));
    }

    @Test
    @DisplayName("Invalid seller sub-order transition throws InvalidOrderStatusTransitionException: PENDING -> SHIPPED")
    void testTransitionSellerOrderStatus_InvalidTransition() {
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                transitionService.transitionSellerOrderStatus(sellerOrder, SellerOrderStatus.SHIPPED, "Skipping steps", user));
    }

    @Test
    @DisplayName("Parent order automatically syncs to DELIVERED when all sub-orders are DELIVERED")
    void testSyncParentOrderStatus_AllDelivered() {
        order.setStatus(OrderStatus.SHIPPED);
        sellerOrder.setStatus(SellerOrderStatus.DELIVERED);

        when(sellerOrderRepository.findByOrderId(order.getId())).thenReturn(List.of(sellerOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        transitionService.syncParentOrderStatus(order, user);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(orderRepository).save(order);
    }
}
