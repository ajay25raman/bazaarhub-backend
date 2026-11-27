package com.bazaarhub.backend.analytics;

import com.bazaarhub.backend.analytics.application.dto.response.*;
import com.bazaarhub.backend.analytics.application.service.SellerAnalyticsServiceImpl;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerAnalyticsServiceUnitTest {

    @Mock
    private SpringDataSellerRepository sellerRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataOrderItemRepository orderItemRepository;

    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    @InjectMocks
    private SellerAnalyticsServiceImpl service;

    private Long userId = 15L;
    private Long sellerId = 3L;
    private SellerEntity seller;

    @BeforeEach
    void setUp() {
        seller = SellerEntity.builder().id(sellerId).build();
    }

    @Test
    @DisplayName("Should return seller overview metrics accurately")
    void shouldReturnSellerOverview() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(sellerOrderRepository.countSellerOrdersBetween(eq(sellerId), any(), any())).thenReturn(40L);
        when(sellerOrderRepository.countSellerOrdersByStatusBetween(eq(sellerId), eq(SellerOrderStatus.PENDING), any(), any())).thenReturn(5L);
        when(sellerOrderRepository.countSellerOrdersByStatusBetween(eq(sellerId), eq(SellerOrderStatus.DELIVERED), any(), any())).thenReturn(32L);
        when(sellerOrderRepository.countSellerOrdersByStatusBetween(eq(sellerId), eq(SellerOrderStatus.CANCELLED), any(), any())).thenReturn(3L);
        when(sellerOrderRepository.sumSubtotalBySellerIdBetween(eq(sellerId), any(), any())).thenReturn(new BigDecimal("4800.00"));
        when(inventoryRepository.countSellerLowStock(sellerId)).thenReturn(2L);
        when(inventoryRepository.countSellerOutOfStock(sellerId)).thenReturn(1L);

        SellerDashboardOverviewResponse response = service.getOverview(userId);

        assertNotNull(response);
        assertEquals(sellerId, response.getSellerId());
        assertEquals(new BigDecimal("4800.00"), response.getTotalRevenue());
        assertEquals(40L, response.getTotalOrders());
        assertEquals(5L, response.getPendingOrders());
        assertEquals(32L, response.getCompletedOrders());
        assertEquals(2L, response.getLowStockProducts());
        assertEquals(1L, response.getOutOfStockProducts());
    }

    @Test
    @DisplayName("Should calculate seller sales, commission deduction, and net payable")
    void shouldCalculateSellerSalesAndPayable() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(sellerOrderRepository.countSellerOrdersBetween(eq(sellerId), any(), any())).thenReturn(20L);
        when(sellerOrderRepository.sumSubtotalBySellerIdBetween(eq(sellerId), any(), any())).thenReturn(new BigDecimal("2000.00"));
        when(sellerOrderRepository.sumCommissionBySellerIdBetween(eq(sellerId), any(), any())).thenReturn(new BigDecimal("200.00"));
        when(sellerOrderRepository.sumTotalAmountBySellerIdBetween(eq(sellerId), any(), any())).thenReturn(new BigDecimal("1800.00"));

        SellerSalesAnalyticsResponse response = service.getSalesAnalytics(userId, null, null);

        assertNotNull(response);
        assertEquals(sellerId, response.getSellerId());
        assertEquals(new BigDecimal("2000.00"), response.getTotalRevenue());
        assertEquals(new BigDecimal("100.00"), response.getAverageOrderValue());
        assertEquals(new BigDecimal("200.00"), response.getCommissionDeducted());
        assertEquals(new BigDecimal("1800.00"), response.getNetPayable());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when non-seller accesses seller analytics")
    void shouldThrowWhenNotSeller() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOverview(999L));
    }
}
