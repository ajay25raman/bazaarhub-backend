package com.bazaarhub.backend.analytics.application.service;

import com.bazaarhub.backend.analytics.application.dto.response.*;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SellerAnalyticsServiceImpl implements SellerAnalyticsService {

    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataOrderItemRepository orderItemRepository;
    private final SpringDataInventoryRepository inventoryRepository;

    public SellerAnalyticsServiceImpl(SpringDataSellerRepository sellerRepository,
                                      SpringDataSellerOrderRepository sellerOrderRepository,
                                      SpringDataOrderItemRepository orderItemRepository,
                                      SpringDataInventoryRepository inventoryRepository) {
        this.sellerRepository = sellerRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    private SellerEntity resolveSeller(Long userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + userId));
    }

    private Instant[] normalizeDateRange(Instant start, Instant end) {
        Instant effectiveEnd = (end != null) ? end : Instant.now();
        Instant effectiveStart = (start != null) ? start : effectiveEnd.minus(30, ChronoUnit.DAYS);
        return new Instant[]{effectiveStart, effectiveEnd};
    }

    @Override
    public SellerDashboardOverviewResponse getOverview(Long userId) {
        SellerEntity seller = resolveSeller(userId);
        Long sellerId = seller.getId();

        Instant[] range = normalizeDateRange(null, null);
        Instant start = range[0];
        Instant end = range[1];

        long totalOrders = sellerOrderRepository.countSellerOrdersBetween(sellerId, start, end);
        long pendingOrders = sellerOrderRepository.countSellerOrdersByStatusBetween(sellerId, SellerOrderStatus.PENDING, start, end);
        long deliveredOrders = sellerOrderRepository.countSellerOrdersByStatusBetween(sellerId, SellerOrderStatus.DELIVERED, start, end);
        long cancelledOrders = sellerOrderRepository.countSellerOrdersByStatusBetween(sellerId, SellerOrderStatus.CANCELLED, start, end);
        BigDecimal totalRevenue = sellerOrderRepository.sumSubtotalBySellerIdBetween(sellerId, start, end);

        long lowStock = inventoryRepository.countSellerLowStock(sellerId);
        long outOfStock = inventoryRepository.countSellerOutOfStock(sellerId);

        return SellerDashboardOverviewResponse.builder()
                .sellerId(sellerId)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .lowStockProducts(lowStock)
                .outOfStockProducts(outOfStock)
                .build();
    }

    @Override
    public SellerSalesAnalyticsResponse getSalesAnalytics(Long userId, Instant start, Instant end) {
        SellerEntity seller = resolveSeller(userId);
        Long sellerId = seller.getId();

        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        long totalOrders = sellerOrderRepository.countSellerOrdersBetween(sellerId, effectiveStart, effectiveEnd);
        BigDecimal totalRevenue = sellerOrderRepository.sumSubtotalBySellerIdBetween(sellerId, effectiveStart, effectiveEnd);
        BigDecimal commission = sellerOrderRepository.sumCommissionBySellerIdBetween(sellerId, effectiveStart, effectiveEnd);
        BigDecimal netPayable = sellerOrderRepository.sumTotalAmountBySellerIdBetween(sellerId, effectiveStart, effectiveEnd);

        BigDecimal aov = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Object[]> dailyData = sellerOrderRepository.aggregateSellerDailySalesTrends(sellerId, effectiveStart, effectiveEnd);
        List<DailySalesDataPoint> dailyTrends = new ArrayList<>();
        if (dailyData != null) {
            for (Object[] row : dailyData) {
                String dateStr = row[0] != null ? row[0].toString() : "";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                BigDecimal rev = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                dailyTrends.add(new DailySalesDataPoint(dateStr, count, rev));
            }
        }

        return SellerSalesAnalyticsResponse.builder()
                .sellerId(sellerId)
                .startDate(effectiveStart)
                .endDate(effectiveEnd)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(aov)
                .commissionDeducted(commission)
                .netPayable(netPayable)
                .dailyTrends(dailyTrends)
                .build();
    }

    @Override
    public SellerProductPerformanceResponse getProductPerformance(Long userId, Instant start, Instant end, int limit) {
        SellerEntity seller = resolveSeller(userId);
        Long sellerId = seller.getId();

        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        int effectiveLimit = limit > 0 ? Math.min(limit, 100) : 10;
        PageRequest pageRequest = PageRequest.of(0, effectiveLimit);

        List<Object[]> rows = orderItemRepository.findSellerTopSellingProducts(sellerId, effectiveStart, effectiveEnd, pageRequest);
        List<ProductSalesItemDto> topProducts = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                Long prodId = row[0] != null ? ((Number) row[0]).longValue() : null;
                String name = row[1] != null ? row[1].toString() : "";
                long qty = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
                topProducts.add(new ProductSalesItemDto(prodId, name, qty, rev));
            }
        }

        return SellerProductPerformanceResponse.builder()
                .sellerId(sellerId)
                .topProducts(topProducts)
                .build();
    }

    @Override
    public SellerOrderBreakdownResponse getOrderBreakdown(Long userId, Instant start, Instant end) {
        SellerEntity seller = resolveSeller(userId);
        Long sellerId = seller.getId();

        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        long totalOrders = sellerOrderRepository.countSellerOrdersBetween(sellerId, effectiveStart, effectiveEnd);
        List<Object[]> statusCounts = sellerOrderRepository.aggregateSellerOrderStatusCounts(sellerId, effectiveStart, effectiveEnd);
        Map<String, Long> breakdown = new HashMap<>();
        long delivered = 0L;
        long cancelled = 0L;

        if (statusCounts != null) {
            for (Object[] row : statusCounts) {
                String status = row[0] != null ? row[0].toString() : "UNKNOWN";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                breakdown.put(status, count);
                if (SellerOrderStatus.DELIVERED.name().equalsIgnoreCase(status)) {
                    delivered += count;
                } else if (SellerOrderStatus.CANCELLED.name().equalsIgnoreCase(status)) {
                    cancelled += count;
                }
            }
        }

        BigDecimal fulfillmentRate = totalOrders > 0
                ? BigDecimal.valueOf(delivered)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return SellerOrderBreakdownResponse.builder()
                .sellerId(sellerId)
                .totalOrders(totalOrders)
                .statusBreakdown(breakdown)
                .deliveredCount(delivered)
                .cancelledCount(cancelled)
                .fulfillmentRate(fulfillmentRate)
                .build();
    }
}
