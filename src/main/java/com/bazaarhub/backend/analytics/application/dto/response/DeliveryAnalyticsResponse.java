package com.bazaarhub.backend.analytics.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAnalyticsResponse {
    private long totalShipments;
    private long deliveredCount;
    private long failedCount;
    private long inTransitCount;
    private BigDecimal deliverySuccessRate;
    private Map<String, Long> statusBreakdown;
}
