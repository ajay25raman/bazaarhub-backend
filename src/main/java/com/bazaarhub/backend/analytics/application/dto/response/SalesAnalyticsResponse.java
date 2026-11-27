package com.bazaarhub.backend.analytics.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalyticsResponse {
    private Instant startDate;
    private Instant endDate;
    private BigDecimal grossMerchandiseValue;
    private BigDecimal netSales;
    private BigDecimal totalRefundAmount;
    private long totalOrders;
    private BigDecimal averageOrderValue;
    private List<DailySalesDataPoint> dailyTrends;
}
