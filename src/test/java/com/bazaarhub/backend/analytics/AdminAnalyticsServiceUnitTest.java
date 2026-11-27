package com.bazaarhub.backend.analytics;

import com.bazaarhub.backend.analytics.application.dto.response.*;
import com.bazaarhub.backend.analytics.application.service.AdminAnalyticsServiceImpl;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataRefundRepository;
import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketStatus;
import com.bazaarhub.backend.support.infrastructure.persistence.repository.SpringDataSupportTicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceUnitTest {

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataOrderItemRepository orderItemRepository;

    @Mock
    private SpringDataRefundRepository refundRepository;

    @Mock
    private SpringDataShipmentRepository shipmentRepository;

    @Mock
    private SpringDataSupportTicketRepository ticketRepository;

    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataProductRepository productRepository;

    @InjectMocks
    private AdminAnalyticsServiceImpl service;

    @Test
    @DisplayName("Should compute overview metrics accurately")
    void shouldComputeOverviewMetrics() {
        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(100L);
        when(orderRepository.sumGrandTotalBetween(any(), any())).thenReturn(new BigDecimal("10000.00"));
        when(refundRepository.sumCompletedRefundsBetween(any(), any())).thenReturn(new BigDecimal("500.00"));
        when(userRepository.countUsersByRoleName("ROLE_CUSTOMER")).thenReturn(450L);
        when(productRepository.count()).thenReturn(120L);
        when(inventoryRepository.countLowStock()).thenReturn(8L);
        when(inventoryRepository.countOutOfStock()).thenReturn(3L);

        AdminOverviewAnalyticsResponse response = service.getOverview();

        assertNotNull(response);
        assertEquals(new BigDecimal("10000.00"), response.getGrossMerchandiseValue());
        assertEquals(new BigDecimal("9500.00"), response.getNetSales());
        assertEquals(100L, response.getTotalOrders());
        assertEquals(new BigDecimal("100.00"), response.getAverageOrderValue());
        assertEquals(450L, response.getTotalCustomers());
        assertEquals(8L, response.getLowStockProducts());
        assertEquals(3L, response.getOutOfStockProducts());
    }

    @Test
    @DisplayName("Should return order metrics and fulfillment rate")
    void shouldReturnOrderMetrics() {
        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(50L);
        when(orderRepository.aggregateOrderStatusCounts(any(), any())).thenReturn(List.of(
                new Object[]{OrderStatus.DELIVERED.name(), 40L},
                new Object[]{OrderStatus.CANCELLED.name(), 5L},
                new Object[]{OrderStatus.CONFIRMED.name(), 5L}
        ));

        OrderMetricsResponse response = service.getOrderMetrics(Instant.now().minusSeconds(86400), Instant.now());

        assertNotNull(response);
        assertEquals(50L, response.getTotalOrders());
        assertEquals(40L, response.getDeliveredCount());
        assertEquals(5L, response.getCancelledCount());
        assertEquals(new BigDecimal("80.00"), response.getFulfillmentRate());
    }

    @Test
    @DisplayName("Should return delivery analytics and success rate")
    void shouldReturnDeliveryAnalytics() {
        when(shipmentRepository.countShipmentsBetween(any(), any())).thenReturn(200L);
        when(shipmentRepository.countShipmentsByStatusBetween(eq(ShipmentStatus.DELIVERED), any(), any())).thenReturn(180L);
        when(shipmentRepository.countShipmentsByStatusBetween(eq(ShipmentStatus.FAILED), any(), any())).thenReturn(10L);
        when(shipmentRepository.countShipmentsByStatusBetween(eq(ShipmentStatus.IN_TRANSIT), any(), any())).thenReturn(10L);
        when(shipmentRepository.aggregateShipmentStatusCounts(any(), any())).thenReturn(List.of(
                new Object[]{ShipmentStatus.DELIVERED.name(), 180L},
                new Object[]{ShipmentStatus.FAILED.name(), 10L}
        ));

        DeliveryAnalyticsResponse response = service.getDeliveryAnalytics(null, null);

        assertNotNull(response);
        assertEquals(200L, response.getTotalShipments());
        assertEquals(180L, response.getDeliveredCount());
        assertEquals(new BigDecimal("90.00"), response.getDeliverySuccessRate());
    }

    @Test
    @DisplayName("Should return support analytics and resolution rate")
    void shouldReturnSupportAnalytics() {
        when(ticketRepository.countTicketsBetween(any(), any())).thenReturn(60L);
        when(ticketRepository.countTicketsByStatusBetween(eq(TicketStatus.OPEN), any(), any())).thenReturn(15L);
        when(ticketRepository.countTicketsByStatusBetween(eq(TicketStatus.RESOLVED), any(), any())).thenReturn(45L);
        when(ticketRepository.aggregateCategoryCounts(any(), any())).thenReturn(List.of(
                new Object[]{TicketCategory.ORDER_ISSUE.name(), 30L},
                new Object[]{TicketCategory.PAYMENT_ISSUE.name(), 30L}
        ));

        SupportAnalyticsResponse response = service.getSupportAnalytics(null, null);

        assertNotNull(response);
        assertEquals(60L, response.getTotalTickets());
        assertEquals(45L, response.getResolvedTickets());
        assertEquals(new BigDecimal("75.00"), response.getResolutionRate());
        assertEquals(30L, response.getTicketsByCategory().get(TicketCategory.ORDER_ISSUE.name()));
    }
}
