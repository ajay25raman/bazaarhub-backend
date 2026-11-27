package com.bazaarhub.backend.shipping.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.bazaarhub.backend.shipping.application.dto.response.PublicTrackingResponse;
import com.bazaarhub.backend.shipping.application.dto.response.TrackingHistoryResponse;
import com.bazaarhub.backend.shipping.application.service.ShipmentTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipments")
@Tag(name = "Shipment Tracking", description = "Endpoints for public tracking number lookup and authenticated shipment history")
public class ShipmentTrackingController {

    private final ShipmentTrackingService shipmentTrackingService;

    public ShipmentTrackingController(ShipmentTrackingService shipmentTrackingService) {
        this.shipmentTrackingService = shipmentTrackingService;
    }

    @GetMapping("/track/{trackingNumber}")
    @Operation(summary = "Public shipment tracking lookup", description = "Publicly accessible endpoint to check shipment status and milestones by tracking number without revealing sensitive customer details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found for tracking number", content = @Content)
    })
    public ResponseEntity<ApiResponse<PublicTrackingResponse>> trackByNumber(
            @Parameter(description = "Tracking number", example = "TRK-2026-ABC123XYZ") @PathVariable String trackingNumber) {
        PublicTrackingResponse response = shipmentTrackingService.trackByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(ApiResponse.ok("Tracking information retrieved successfully", response));
    }

    @GetMapping("/{shipmentId}/tracking-history")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyAuthority('ORDER_READ', 'SELLER_ORDER_MANAGE', 'ROLE_CUSTOMER', 'ROLE_SELLER', 'ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Get full tracking history", description = "Authenticated endpoint to retrieve full transit milestones and GPS breadcrumb trail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have access to this shipment", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<TrackingHistoryResponse>> getTrackingHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = principal != null ? principal.getId() : null;

        TrackingHistoryResponse response = shipmentTrackingService.getTrackingHistory(userId, shipmentId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Tracking history retrieved successfully", response));
    }
}
