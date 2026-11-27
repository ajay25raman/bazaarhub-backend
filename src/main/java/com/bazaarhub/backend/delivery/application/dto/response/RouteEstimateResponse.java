package com.bazaarhub.backend.delivery.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Route distance and duration estimate computed via Google Maps")
public class RouteEstimateResponse {

    @Schema(description = "Estimated road distance in kilometers", example = "6.45")
    private BigDecimal distanceKm;

    @Schema(description = "Estimated transit duration in minutes", example = "22")
    private int durationMinutes;

    @Schema(description = "Route summary or routing engine description", example = "Google Maps Route via Eastern Express Highway")
    private String summary;
}
