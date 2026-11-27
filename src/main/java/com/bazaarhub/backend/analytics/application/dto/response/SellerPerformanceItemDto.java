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
public class SellerPerformanceItemDto {
    private Long sellerId;
    private String storeName;
    private long orderCount;
    private BigDecimal totalGmv;
}
