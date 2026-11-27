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
public class CategorySalesItemDto {
    private Long categoryId;
    private String categoryName;
    private long orderCount;
    private long quantitySold;
    private BigDecimal revenue;
}
