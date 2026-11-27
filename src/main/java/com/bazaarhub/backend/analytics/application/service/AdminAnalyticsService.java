package com.bazaarhub.backend.analytics.application.service;

import com.bazaarhub.backend.analytics.application.dto.response.*;

import java.time.Instant;

public interface AdminAnalyticsService {

    AdminOverviewAnalyticsResponse getOverview();

    SalesAnalyticsResponse getSalesAnalytics(Instant start, Instant end);

    OrderMetricsResponse getOrderMetrics(Instant start, Instant end);

    ProductAnalyticsResponse getProductAnalytics(Instant start, Instant end, int limit);

    CategoryAnalyticsResponse getCategoryPerformance(Instant start, Instant end);

    SellerPerformanceResponse getSellerPerformance(Instant start, Instant end, int limit);

    CustomerAnalyticsResponse getCustomerAnalytics(Instant start, Instant end);

    DeliveryAnalyticsResponse getDeliveryAnalytics(Instant start, Instant end);

    SupportAnalyticsResponse getSupportAnalytics(Instant start, Instant end);
}
