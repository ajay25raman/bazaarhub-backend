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
public class CustomerAnalyticsResponse {
    private long totalCustomers;
    private long newCustomersInPeriod;
    private long activeCustomersInPeriod;
    private long repeatCustomersCount;
    private BigDecimal repeatPurchaseRate;
}
