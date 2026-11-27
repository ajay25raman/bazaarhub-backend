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
public class SellerOrderBreakdownResponse {
    private Long sellerId;
    private long totalOrders;
    private Map<String, Long> statusBreakdown;
    private long deliveredCount;
    private long cancelledCount;
    private BigDecimal fulfillmentRate;
}
