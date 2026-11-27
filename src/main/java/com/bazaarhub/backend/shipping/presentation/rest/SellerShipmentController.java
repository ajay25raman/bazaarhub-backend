package com.bazaarhub.backend.shipping.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.bazaarhub.backend.shipping.application.dto.response.InternalShipmentResponse;
import com.bazaarhub.backend.shipping.application.service.InternalShippingService;
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

@RestController
@RequestMapping("/api/v1/shipments/seller")
@Tag(name = "Seller Shipment Operations", description = "Endpoints for merchant sellers to view and manage dispatch shipments for their orders")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_SELLER', 'ROLE_ADMIN')")
public class SellerShipmentController {

    private final InternalShippingService internalShippingService;

    public SellerShipmentController(InternalShippingService internalShippingService) {
        this.internalShippingService = internalShippingService;
    }

    @GetMapping
    @Operation(summary = "List seller shipments", description = "Retrieves paginated list of shipments for the authenticated seller's store")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InternalShipmentResponse>>> getSellerShipments(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by shipment status") @RequestParam(required = false) ShipmentStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InternalShipmentResponse> page = internalShippingService.getSellerShipments(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Seller shipments retrieved successfully", page));
    }

    @GetMapping("/{shipmentId}")
    @Operation(summary = "Get seller shipment details", description = "Retrieves complete shipment information for a specific store dispatch")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller shipment retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Shipment belongs to another seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InternalShipmentResponse>> getSellerShipmentById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        InternalShipmentResponse response = internalShippingService.getSellerShipmentById(principal.getId(), shipmentId);
        return ResponseEntity.ok(ApiResponse.ok("Seller shipment retrieved successfully", response));
    }
}
