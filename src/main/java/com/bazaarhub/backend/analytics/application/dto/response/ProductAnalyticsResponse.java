package com.bazaarhub.backend.analytics.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalyticsResponse {
    private long totalProducts;
    private long lowStockCount;
    private long outOfStockCount;
    private List<ProductSalesItemDto> topSellingByVolume;
    private List<ProductSalesItemDto> topSellingByRevenue;
}
