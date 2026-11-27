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
public class SellerSalesAnalyticsResponse {
    private Long sellerId;
    private Instant startDate;
    private Instant endDate;
    private BigDecimal totalRevenue;
    private long totalOrders;
    private BigDecimal averageOrderValue;
    private BigDecimal commissionDeducted;
    private BigDecimal netPayable;
    private List<DailySalesDataPoint> dailyTrends;
}
