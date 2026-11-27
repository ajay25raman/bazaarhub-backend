package com.bazaarhub.backend.analytics.presentation.rest;

import com.bazaarhub.backend.analytics.application.dto.response.*;
import com.bazaarhub.backend.analytics.application.service.SellerAnalyticsService;
import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/seller/analytics")
@Tag(name = "Seller Analytics", description = "Merchant operational dashboards, sales performance, and catalog inventory metrics (strictly isolated per merchant)")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
public class SellerAnalyticsController {

    private final SellerAnalyticsService analyticsService;

    public SellerAnalyticsController(SellerAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Seller dashboard executive overview", description = "Merchant-scoped revenue, sub-order counts, pending fulfillments, and stock warnings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller overview retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerDashboardOverviewResponse>> getOverview(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {

        SellerDashboardOverviewResponse response = analyticsService.getOverview(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Seller overview retrieved successfully", response));
    }

    @GetMapping("/sales")
    @Operation(summary = "Seller sales performance and daily revenue", description = "Daily revenue, orders, commission deductions, and net seller payable")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller sales analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerSalesAnalyticsResponse>> getSales(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        SellerSalesAnalyticsResponse response = analyticsService.getSalesAnalytics(principal.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Seller sales analytics retrieved successfully", response));
    }

    @GetMapping("/products")
    @Operation(summary = "Seller top performing products", description = "Ranked list of seller's products by volume and revenue")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller product performance retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format or limit parameter", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerProductPerformanceResponse>> getProducts(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @Parameter(description = "Maximum products to return", example = "10") @RequestParam(defaultValue = "10") int limit) {

        SellerProductPerformanceResponse response = analyticsService.getProductPerformance(principal.getId(), startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.ok("Seller product performance retrieved successfully", response));
    }

    @GetMapping("/orders")
    @Operation(summary = "Seller order status breakdown and fulfillment rate", description = "Sub-order status distribution and completed fulfillment percentage")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller order breakdown retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerOrderBreakdownResponse>> getOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        SellerOrderBreakdownResponse response = analyticsService.getOrderBreakdown(principal.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Seller order breakdown retrieved successfully", response));
    }
}
