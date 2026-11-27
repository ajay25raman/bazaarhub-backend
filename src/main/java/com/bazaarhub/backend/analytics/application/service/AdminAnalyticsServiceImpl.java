package com.bazaarhub.backend.analytics.application.service;

import com.bazaarhub.backend.analytics.application.dto.response.*;
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
import com.bazaarhub.backend.support.domain.model.TicketStatus;
import com.bazaarhub.backend.support.infrastructure.persistence.repository.SpringDataSupportTicketRepository;
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
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataOrderItemRepository orderItemRepository;
    private final SpringDataRefundRepository refundRepository;
    private final SpringDataShipmentRepository shipmentRepository;
    private final SpringDataSupportTicketRepository ticketRepository;
    private final SpringDataInventoryRepository inventoryRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataProductRepository productRepository;

    public AdminAnalyticsServiceImpl(SpringDataOrderRepository orderRepository,
                                     SpringDataSellerOrderRepository sellerOrderRepository,
                                     SpringDataOrderItemRepository orderItemRepository,
                                     SpringDataRefundRepository refundRepository,
                                     SpringDataShipmentRepository shipmentRepository,
                                     SpringDataSupportTicketRepository ticketRepository,
                                     SpringDataInventoryRepository inventoryRepository,
                                     SpringDataUserRepository userRepository,
                                     SpringDataProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.refundRepository = refundRepository;
        this.shipmentRepository = shipmentRepository;
        this.ticketRepository = ticketRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    private Instant[] normalizeDateRange(Instant start, Instant end) {
        Instant effectiveEnd = (end != null) ? end : Instant.now();
        Instant effectiveStart = (start != null) ? start : effectiveEnd.minus(30, ChronoUnit.DAYS);
        return new Instant[]{effectiveStart, effectiveEnd};
    }

    @Override
    public AdminOverviewAnalyticsResponse getOverview() {
        Instant[] range = normalizeDateRange(null, null);
        Instant start = range[0];
        Instant end = range[1];

        long totalOrders = orderRepository.countOrdersBetween(start, end);
        BigDecimal gmv = orderRepository.sumGrandTotalBetween(start, end);
        BigDecimal totalRefunds = refundRepository.sumCompletedRefundsBetween(start, end);
        BigDecimal netSales = gmv.subtract(totalRefunds);

        BigDecimal aov = totalOrders > 0
                ? gmv.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalCustomers = userRepository.countUsersByRoleName("ROLE_CUSTOMER");
        long totalProducts = productRepository.count();
        long lowStock = inventoryRepository.countLowStock();
        long outOfStock = inventoryRepository.countOutOfStock();

        return AdminOverviewAnalyticsResponse.builder()
                .grossMerchandiseValue(gmv)
                .netSales(netSales)
                .totalOrders(totalOrders)
                .averageOrderValue(aov)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStock)
                .outOfStockProducts(outOfStock)
                .totalRefundAmount(totalRefunds)
                .build();
    }

    @Override
    public SalesAnalyticsResponse getSalesAnalytics(Instant start, Instant end) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        long totalOrders = orderRepository.countOrdersBetween(effectiveStart, effectiveEnd);
        BigDecimal gmv = orderRepository.sumGrandTotalBetween(effectiveStart, effectiveEnd);
        BigDecimal refunds = refundRepository.sumCompletedRefundsBetween(effectiveStart, effectiveEnd);
        BigDecimal netSales = gmv.subtract(refunds);

        BigDecimal aov = totalOrders > 0
                ? gmv.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Object[]> dailyData = orderRepository.aggregateDailySalesTrends(effectiveStart, effectiveEnd);
        List<DailySalesDataPoint> dailyTrends = new ArrayList<>();
        if (dailyData != null) {
            for (Object[] row : dailyData) {
                String dateStr = row[0] != null ? row[0].toString() : "";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                BigDecimal rev = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                dailyTrends.add(new DailySalesDataPoint(dateStr, count, rev));
            }
        }

        return SalesAnalyticsResponse.builder()
                .startDate(effectiveStart)
                .endDate(effectiveEnd)
                .grossMerchandiseValue(gmv)
                .netSales(netSales)
                .totalRefundAmount(refunds)
                .totalOrders(totalOrders)
                .averageOrderValue(aov)
                .dailyTrends(dailyTrends)
                .build();
    }

    @Override
    public OrderMetricsResponse getOrderMetrics(Instant start, Instant end) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        long totalOrders = orderRepository.countOrdersBetween(effectiveStart, effectiveEnd);
        List<Object[]> statusCounts = orderRepository.aggregateOrderStatusCounts(effectiveStart, effectiveEnd);
        Map<String, Long> breakdown = new HashMap<>();
        long delivered = 0L;
        long cancelled = 0L;

        if (statusCounts != null) {
            for (Object[] row : statusCounts) {
                String status = row[0] != null ? row[0].toString() : "UNKNOWN";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                breakdown.put(status, count);
                if (OrderStatus.DELIVERED.name().equalsIgnoreCase(status)) {
                    delivered += count;
                } else if (OrderStatus.CANCELLED.name().equalsIgnoreCase(status)) {
                    cancelled += count;
                }
            }
        }

        BigDecimal fulfillmentRate = totalOrders > 0
                ? BigDecimal.valueOf(delivered)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return OrderMetricsResponse.builder()
                .totalOrders(totalOrders)
                .statusBreakdown(breakdown)
                .deliveredCount(delivered)
                .cancelledCount(cancelled)
                .fulfillmentRate(fulfillmentRate)
                .build();
    }

    @Override
    public ProductAnalyticsResponse getProductAnalytics(Instant start, Instant end, int limit) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        int effectiveLimit = limit > 0 ? Math.min(limit, 100) : 10;
        PageRequest pageRequest = PageRequest.of(0, effectiveLimit);

        List<Object[]> volumeRows = orderItemRepository.findTopSellingProductsByVolume(effectiveStart, effectiveEnd, pageRequest);
        List<ProductSalesItemDto> topVolume = mapProductSalesRows(volumeRows);

        List<Object[]> revenueRows = orderItemRepository.findTopSellingProductsByRevenue(effectiveStart, effectiveEnd, pageRequest);
        List<ProductSalesItemDto> topRevenue = mapProductSalesRows(revenueRows);

        long totalProducts = productRepository.count();
        long lowStock = inventoryRepository.countLowStock();
        long outOfStock = inventoryRepository.countOutOfStock();

        return ProductAnalyticsResponse.builder()
                .totalProducts(totalProducts)
                .lowStockCount(lowStock)
                .outOfStockCount(outOfStock)
                .topSellingByVolume(topVolume)
                .topSellingByRevenue(topRevenue)
                .build();
    }

    private List<ProductSalesItemDto> mapProductSalesRows(List<Object[]> rows) {
        List<ProductSalesItemDto> list = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
                String name = row[1] != null ? row[1].toString() : "";
                long qty = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
                list.add(new ProductSalesItemDto(id, name, qty, rev));
            }
        }
        return list;
    }

    @Override
    public CategoryAnalyticsResponse getCategoryPerformance(Instant start, Instant end) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        List<Object[]> rows = orderItemRepository.aggregateCategorySalesPerformance(effectiveStart, effectiveEnd);
        List<CategorySalesItemDto> performance = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                Long catId = row[0] != null ? ((Number) row[0]).longValue() : null;
                String name = row[1] != null ? row[1].toString() : "";
                long orderCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                long qty = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                BigDecimal rev = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;
                performance.add(new CategorySalesItemDto(catId, name, orderCount, qty, rev));
            }
        }

        return CategoryAnalyticsResponse.builder()
                .categoryPerformance(performance)
                .build();
    }

    @Override
    public SellerPerformanceResponse getSellerPerformance(Instant start, Instant end, int limit) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        int effectiveLimit = limit > 0 ? Math.min(limit, 100) : 10;
        PageRequest pageRequest = PageRequest.of(0, effectiveLimit);

        List<Object[]> rows = sellerOrderRepository.findTopSellersByGMV(effectiveStart, effectiveEnd, pageRequest);
        List<SellerPerformanceItemDto> topSellers = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                Long sellerId = row[0] != null ? ((Number) row[0]).longValue() : null;
                String storeName = row[1] != null ? row[1].toString() : "";
                long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                BigDecimal gmv = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
                topSellers.add(new SellerPerformanceItemDto(sellerId, storeName, count, gmv));
            }
        }

        return SellerPerformanceResponse.builder()
                .topSellers(topSellers)
                .build();
    }

    @Override
    public CustomerAnalyticsResponse getCustomerAnalytics(Instant start, Instant end) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        long totalCustomers = userRepository.countUsersByRoleName("ROLE_CUSTOMER");
        long newCustomers = userRepository.countUsersCreatedBetween(effectiveStart, effectiveEnd);
        long activeCustomers = orderRepository.countDistinctCustomersBetween(effectiveStart, effectiveEnd);
        long repeatCustomers = orderRepository.countRepeatCustomersBetween(effectiveStart, effectiveEnd);

        BigDecimal repeatRate = activeCustomers > 0
                ? BigDecimal.valueOf(repeatCustomers)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(activeCustomers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return CustomerAnalyticsResponse.builder()
                .totalCustomers(totalCustomers)
                .newCustomersInPeriod(newCustomers)
                .activeCustomersInPeriod(activeCustomers)
                .repeatCustomersCount(repeatCustomers)
                .repeatPurchaseRate(repeatRate)
                .build();
    }

    @Override
    public DeliveryAnalyticsResponse getDeliveryAnalytics(Instant start, Instant end) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        long totalShipments = shipmentRepository.countShipmentsBetween(effectiveStart, effectiveEnd);
        long delivered = shipmentRepository.countShipmentsByStatusBetween(ShipmentStatus.DELIVERED, effectiveStart, effectiveEnd);
        long failed = shipmentRepository.countShipmentsByStatusBetween(ShipmentStatus.FAILED, effectiveStart, effectiveEnd);
        long inTransit = shipmentRepository.countShipmentsByStatusBetween(ShipmentStatus.IN_TRANSIT, effectiveStart, effectiveEnd);

        List<Object[]> statusRows = shipmentRepository.aggregateShipmentStatusCounts(effectiveStart, effectiveEnd);
        Map<String, Long> statusBreakdown = new HashMap<>();
        if (statusRows != null) {
            for (Object[] row : statusRows) {
                String status = row[0] != null ? row[0].toString() : "UNKNOWN";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                statusBreakdown.put(status, count);
            }
        }

        BigDecimal successRate = totalShipments > 0
                ? BigDecimal.valueOf(delivered)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalShipments), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DeliveryAnalyticsResponse.builder()
                .totalShipments(totalShipments)
                .deliveredCount(delivered)
                .failedCount(failed)
                .inTransitCount(inTransit)
                .deliverySuccessRate(successRate)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    @Override
    public SupportAnalyticsResponse getSupportAnalytics(Instant start, Instant end) {
        Instant[] range = normalizeDateRange(start, end);
        Instant effectiveStart = range[0];
        Instant effectiveEnd = range[1];

        long totalTickets = ticketRepository.countTicketsBetween(effectiveStart, effectiveEnd);
        long openTickets = ticketRepository.countTicketsByStatusBetween(TicketStatus.OPEN, effectiveStart, effectiveEnd);
        long resolvedTickets = ticketRepository.countTicketsByStatusBetween(TicketStatus.RESOLVED, effectiveStart, effectiveEnd);

        List<Object[]> catRows = ticketRepository.aggregateCategoryCounts(effectiveStart, effectiveEnd);
        Map<String, Long> ticketsByCategory = new HashMap<>();
        if (catRows != null) {
            for (Object[] row : catRows) {
                String cat = row[0] != null ? row[0].toString() : "OTHER";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                ticketsByCategory.put(cat, count);
            }
        }

        List<Object[]> statusRows = ticketRepository.aggregateTicketStatusCounts(effectiveStart, effectiveEnd);
        Map<String, Long> ticketsByStatus = new HashMap<>();
        if (statusRows != null) {
            for (Object[] row : statusRows) {
                String status = row[0] != null ? row[0].toString() : "OPEN";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                ticketsByStatus.put(status, count);
            }
        }

        BigDecimal resolutionRate = totalTickets > 0
                ? BigDecimal.valueOf(resolvedTickets)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalTickets), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return SupportAnalyticsResponse.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .resolvedTickets(resolvedTickets)
                .resolutionRate(resolutionRate)
                .ticketsByCategory(ticketsByCategory)
                .ticketsByStatus(ticketsByStatus)
                .build();
    }
}
