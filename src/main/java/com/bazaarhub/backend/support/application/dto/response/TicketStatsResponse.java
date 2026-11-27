package com.bazaarhub.backend.support.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated count of support tickets across lifecycle states")
public class TicketStatsResponse {

    @Schema(description = "Count of tickets in OPEN status", example = "12")
    private long openCount;

    @Schema(description = "Count of tickets currently IN_PROGRESS", example = "8")
    private long inProgressCount;

    @Schema(description = "Count of tickets WAITING_FOR_CUSTOMER response", example = "5")
    private long waitingForCustomerCount;

    @Schema(description = "Count of RESOLVED tickets", example = "45")
    private long resolvedCount;

    @Schema(description = "Count of CLOSED tickets", example = "120")
    private long closedCount;

    @Schema(description = "Total tickets count", example = "190")
    private long totalCount;
}
