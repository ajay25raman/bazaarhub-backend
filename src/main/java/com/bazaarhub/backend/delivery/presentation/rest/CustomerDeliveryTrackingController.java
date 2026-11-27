package com.bazaarhub.backend.delivery.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryTrackingResponse;
import com.bazaarhub.backend.delivery.application.dto.response.RouteEstimateResponse;
import com.bazaarhub.backend.delivery.application.dto.response.ShipmentResponse;
import com.bazaarhub.backend.delivery.application.service.DeliveryTrackingService;
import com.bazaarhub.backend.delivery.application.service.ShipmentService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Customer Shipment Tracking and Location Inspection.
 */
@RestController
@RequestMapping("/api/v1/delivery/tracking")
@Tag(name = "Customer Delivery Tracking", description = "Endpoints for customers to track their package live location and estimated arrival time")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CustomerDeliveryTrackingController {

    private final DeliveryTrackingService deliveryTrackingService;
    private final ShipmentService shipmentService;

    public CustomerDeliveryTrackingController(DeliveryTrackingService deliveryTrackingService,
                                              ShipmentService shipmentService) {
        this.deliveryTrackingService = deliveryTrackingService;
        this.shipmentService = shipmentService;
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER', 'ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Get latest live location of shipment", description = "Retrieves the most recent GPS location of the delivery partner")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Latest location retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not own the order or lack shipment access", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found or no location recorded", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryTrackingResponse>> getLatestLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = principal != null ? principal.getId() : null;

        DeliveryTrackingResponse response = deliveryTrackingService.getLatestLocation(userId, shipmentId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Latest location retrieved successfully", response));
    }

    @GetMapping("/{shipmentId}/estimate")
    @PreAuthorize("hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER', 'ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Get estimated time of arrival", description = "Retrieves distance and estimated delivery duration in minutes")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ETA calculated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not own the order or lack shipment access", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found or route cannot be computed", content = @Content)
    })
    public ResponseEntity<ApiResponse<RouteEstimateResponse>> getEstimatedArrival(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = principal != null ? principal.getId() : null;

        RouteEstimateResponse response = deliveryTrackingService.getRouteEstimate(userId, shipmentId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("ETA calculated successfully", response));
    }

    @GetMapping("/shipments/my")
    @PreAuthorize("hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
    @Operation(summary = "Get my package shipments", description = "Retrieves paginated list of shipments for orders placed by authenticated customer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<ShipmentResponse>>> getMyShipments(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ShipmentResponse> page = shipmentService.getCustomerShipments(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Customer shipments retrieved successfully", page));
    }
}
