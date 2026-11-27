package com.bazaarhub.backend.shipping.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
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
@RequestMapping("/api/v1/customer/shipments")
@Tag(name = "Customer Shipments", description = "Endpoints for authenticated customers to view their package shipments and deliveries")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerShipmentController {

    private final InternalShippingService internalShippingService;

    public CustomerShipmentController(InternalShippingService internalShippingService) {
        this.internalShippingService = internalShippingService;
    }

    @GetMapping
    @Operation(summary = "List customer shipments", description = "Retrieves paginated list of all package shipments for the authenticated customer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InternalShipmentResponse>>> getCustomerShipments(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InternalShipmentResponse> page = internalShippingService.getCustomerShipments(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Customer shipments retrieved successfully", page));
    }

    @GetMapping("/{shipmentId}")
    @Operation(summary = "Get customer shipment details", description = "Retrieves details of a specific package shipment owned by the authenticated customer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer shipment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Shipment belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InternalShipmentResponse>> getCustomerShipmentById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        InternalShipmentResponse response = internalShippingService.getCustomerShipmentById(principal.getId(), shipmentId);
        return ResponseEntity.ok(ApiResponse.ok("Customer shipment details retrieved successfully", response));
    }
}
