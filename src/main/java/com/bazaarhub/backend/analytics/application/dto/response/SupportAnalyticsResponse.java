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
public class SupportAnalyticsResponse {
    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private BigDecimal resolutionRate;
    private Map<String, Long> ticketsByCategory;
    private Map<String, Long> ticketsByStatus;
}
