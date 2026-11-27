package com.bazaarhub.backend.analytics.application.service;

import com.bazaarhub.backend.analytics.application.dto.response.*;

import java.time.Instant;

public interface SellerAnalyticsService {

    SellerDashboardOverviewResponse getOverview(Long userId);

    SellerSalesAnalyticsResponse getSalesAnalytics(Long userId, Instant start, Instant end);

    SellerProductPerformanceResponse getProductPerformance(Long userId, Instant start, Instant end, int limit);

    SellerOrderBreakdownResponse getOrderBreakdown(Long userId, Instant start, Instant end);
}
