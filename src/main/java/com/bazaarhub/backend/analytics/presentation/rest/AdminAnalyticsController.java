package com.bazaarhub.backend.analytics.presentation.rest;

import com.bazaarhub.backend.analytics.application.dto.response.*;
import com.bazaarhub.backend.analytics.application.service.AdminAnalyticsService;
import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@Tag(name = "Admin Analytics & Reporting", description = "Platform-wide analytical dashboards, sales intelligence, and operational metrics")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Platform-level high-level executive overview", description = "Aggregated GMV, net revenue, total orders, customers, and active product stock indicators")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Platform overview retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<AdminOverviewAnalyticsResponse>> getOverview() {
        AdminOverviewAnalyticsResponse response = analyticsService.getOverview();
        return ResponseEntity.ok(ApiResponse.ok("Platform overview retrieved successfully", response));
    }

    @GetMapping("/sales")
    @Operation(summary = "Sales metrics and daily revenue time series", description = "Gross merchandise value, net sales, refund deductions, and daily revenue breakdown")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sales analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SalesAnalyticsResponse>> getSales(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        SalesAnalyticsResponse response = analyticsService.getSalesAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Sales analytics retrieved successfully", response));
    }

    @GetMapping("/orders")
    @Operation(summary = "Order fulfillment and volume breakdown", description = "Order status distribution, completion rate, and cancellation volume")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order metrics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<OrderMetricsResponse>> getOrders(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        OrderMetricsResponse response = analyticsService.getOrderMetrics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Order metrics retrieved successfully", response));
    }

    @GetMapping("/products")
    @Operation(summary = "Catalog product performance and inventory health", description = "Top selling products by volume and revenue with inventory health metrics")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format or limit parameter", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductAnalyticsResponse>> getProducts(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @Parameter(description = "Maximum products to return", example = "10") @RequestParam(defaultValue = "10") int limit) {

        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.ok("Product analytics retrieved successfully", response));
    }

    @GetMapping("/categories")
    @Operation(summary = "Category sales performance", description = "Revenue and units sold breakdown across product categories")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<CategoryAnalyticsResponse>> getCategories(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        CategoryAnalyticsResponse response = analyticsService.getCategoryPerformance(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Category performance retrieved successfully", response));
    }

    @GetMapping("/sellers")
    @Operation(summary = "Top performing sellers leaderboard", description = "Ranked merchant list by GMV and completed volume")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller performance retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format or limit parameter", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerPerformanceResponse>> getSellers(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @Parameter(description = "Maximum sellers to return", example = "10") @RequestParam(defaultValue = "10") int limit) {

        SellerPerformanceResponse response = analyticsService.getSellerPerformance(startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.ok("Seller performance retrieved successfully", response));
    }

    @GetMapping("/customers")
    @Operation(summary = "Customer cohort metrics and retention", description = "Total customers, period signups, active buyers, and repeat purchase rate")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerAnalyticsResponse>> getCustomers(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        CustomerAnalyticsResponse response = analyticsService.getCustomerAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Customer analytics retrieved successfully", response));
    }

    @GetMapping("/delivery")
    @Operation(summary = "Logistics and shipping performance", description = "Delivery success rate, transit volume, and status distribution")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAnalyticsResponse>> getDelivery(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        DeliveryAnalyticsResponse response = analyticsService.getDeliveryAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Delivery analytics retrieved successfully", response));
    }

    @GetMapping("/support")
    @Operation(summary = "Customer support workload and resolution efficiency", description = "Ticket volume, category breakdown, open vs resolved ratio, and resolution rate")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Support analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportAnalyticsResponse>> getSupport(
            @Parameter(description = "Start datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End datetime in ISO-8601 UTC format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        SupportAnalyticsResponse response = analyticsService.getSupportAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Support analytics retrieved successfully", response));
    }
}
