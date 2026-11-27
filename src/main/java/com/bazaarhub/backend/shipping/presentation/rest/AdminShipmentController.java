package com.bazaarhub.backend.shipping.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.bazaarhub.backend.shipping.application.dto.request.CancelShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.response.InternalShipmentResponse;
import com.bazaarhub.backend.shipping.application.service.InternalShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/shipments")
@Tag(name = "Admin Shipment Management", description = "Endpoints for administrators to audit, monitor, and manage platform logistics shipments")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasRole('ADMIN')")
public class AdminShipmentController {

    private final InternalShippingService internalShippingService;

    public AdminShipmentController(InternalShippingService internalShippingService) {
        this.internalShippingService = internalShippingService;
    }

    @GetMapping
    @Operation(summary = "List all platform shipments", description = "Retrieves paginated shipments with optional keyword search and filters by status, seller, and customer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Platform shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InternalShipmentResponse>>> listAllShipments(
            @Parameter(description = "Keyword search across tracking numbers and addresses") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by shipment status") @RequestParam(required = false) ShipmentStatus status,
            @Parameter(description = "Filter by seller ID") @RequestParam(required = false) Long sellerId,
            @Parameter(description = "Filter by customer ID") @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InternalShipmentResponse> page = internalShippingService.adminListShipments(search, status, sellerId, customerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Shipments retrieved successfully", page));
    }

    @GetMapping("/{shipmentId}")
    @Operation(summary = "Get shipment details", description = "Retrieves full administrative shipment record with all related entities")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InternalShipmentResponse>> getShipmentById(
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        InternalShipmentResponse response = internalShippingService.getShipmentById(null, shipmentId, true);
        return ResponseEntity.ok(ApiResponse.ok("Shipment details retrieved successfully", response));
    }

    @GetMapping("/active")
    @Operation(summary = "List active in-flight shipments", description = "Retrieves shipments in READY_FOR_PICKUP, PICKED_UP, IN_TRANSIT, or OUT_FOR_DELIVERY status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InternalShipmentResponse>>> listActiveShipments(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InternalShipmentResponse> page = internalShippingService.adminListActiveShipments(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Active shipments retrieved successfully", page));
    }

    @GetMapping("/failed")
    @Operation(summary = "List failed shipments", description = "Retrieves shipments where delivery attempt failed and requires operational review or re-dispatch")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Failed shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InternalShipmentResponse>>> listFailedShipments(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InternalShipmentResponse> page = internalShippingService.adminListFailedShipments(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Failed shipments retrieved successfully", page));
    }

    @PostMapping("/{shipmentId}/cancel")
    @Operation(summary = "Cancel shipment", description = "Administratively cancels an active shipment and its assigned delivery task")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment cancelled successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Delivered shipment cannot be cancelled", content = @Content)
    })
    public ResponseEntity<ApiResponse<InternalShipmentResponse>> cancelShipment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId,
            @Valid @RequestBody CancelShipmentRequest request) {
        InternalShipmentResponse response = internalShippingService.cancelShipment(principal.getId(), shipmentId, request, true);
        return ResponseEntity.ok(ApiResponse.ok("Shipment cancelled successfully", response));
    }
}
