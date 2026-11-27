package com.bazaarhub.backend.analytics.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewAnalyticsResponse {
    private BigDecimal grossMerchandiseValue;
    private BigDecimal netSales;
    private long totalOrders;
    private BigDecimal averageOrderValue;
    private long totalCustomers;
    private long totalProducts;
    private long lowStockProducts;
    private long outOfStockProducts;
    private BigDecimal totalRefundAmount;
}
