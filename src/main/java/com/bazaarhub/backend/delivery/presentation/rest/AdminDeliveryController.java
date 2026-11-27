package com.bazaarhub.backend.delivery.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.dto.response.*;
import com.bazaarhub.backend.delivery.application.service.DeliveryAssignmentService;
import com.bazaarhub.backend.delivery.application.service.DeliveryBoyService;
import com.bazaarhub.backend.delivery.application.service.DeliveryTrackingService;
import com.bazaarhub.backend.delivery.application.service.ShipmentService;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Administrative Delivery Operations:
 * Delivery boy verification, account suspension, manual shipment creation,
 * assignment/reassignment, and fleet audit tracking.
 */
@RestController
@RequestMapping("/api/v1/admin/delivery")
@Tag(name = "Admin Delivery Management", description = "Endpoints for administrators to manage delivery personnel, verify KYC, assign shipments, and inspect delivery operations")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final DeliveryBoyService deliveryBoyService;
    private final ShipmentService shipmentService;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final DeliveryTrackingService deliveryTrackingService;

    public AdminDeliveryController(DeliveryBoyService deliveryBoyService,
                                   ShipmentService shipmentService,
                                   DeliveryAssignmentService deliveryAssignmentService,
                                   DeliveryTrackingService deliveryTrackingService) {
        this.deliveryBoyService = deliveryBoyService;
        this.shipmentService = shipmentService;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.deliveryTrackingService = deliveryTrackingService;
    }

    @GetMapping("/boys")
    @Operation(summary = "List delivery partners", description = "Retrieves paginated list of delivery partners with optional verification and availability filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery partners retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<DeliveryBoyResponse>>> listDeliveryBoys(
            @Parameter(description = "Filter by verification status") @RequestParam(required = false) DeliveryBoyVerificationStatus verificationStatus,
            @Parameter(description = "Filter by availability status") @RequestParam(required = false) DeliveryBoyAvailabilityStatus availabilityStatus,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DeliveryBoyResponse> page = deliveryBoyService.adminListDeliveryBoys(verificationStatus, availabilityStatus, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partners retrieved successfully", page));
    }

    @GetMapping("/boys/{id}")
    @Operation(summary = "Get delivery partner details", description = "Retrieves profile and operational details of a specific delivery partner")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery partner retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery partner not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> getDeliveryBoyById(
            @Parameter(description = "Delivery partner ID", example = "1") @PathVariable Long id) {
        DeliveryBoyResponse response = deliveryBoyService.getDeliveryBoyById(id);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner retrieved successfully", response));
    }

    @PatchMapping("/boys/{id}/verify")
    @Operation(summary = "Verify delivery partner KYC", description = "Approves or rejects delivery partner onboarding and KYC documentation")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery partner KYC updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or missing remarks", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery partner not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Invalid verification status transition", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> verifyDeliveryBoy(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Delivery partner ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody AdminVerifyDeliveryBoyRequest request) {
        DeliveryBoyResponse response = deliveryBoyService.adminVerifyDeliveryBoy(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner KYC updated successfully", response));
    }

    @PatchMapping("/boys/{id}/status")
    @Operation(summary = "Update delivery partner status", description = "Suspends or activates a delivery partner account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery partner status updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery partner not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> updateDeliveryBoyStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Delivery partner ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody AdminUpdateDeliveryBoyStatusRequest request) {
        DeliveryBoyResponse response = deliveryBoyService.adminUpdateStatus(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner status updated successfully", response));
    }

    @PostMapping("/shipments")
    @Operation(summary = "Create shipment for seller sub-order", description = "Creates a shipment dispatch record for a confirmed seller order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Shipment created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid sub-order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller order not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Shipment already exists for this sub-order", content = @Content)
    })
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shipmentService.createShipment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shipment created successfully", response));
    }

    @GetMapping("/shipments")
    @Operation(summary = "List shipments", description = "Retrieves paginated list of shipments with optional filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<ShipmentResponse>>> listShipments(
            @Parameter(description = "Filter by shipment status") @RequestParam(required = false) ShipmentStatus status,
            @Parameter(description = "Filter by seller order ID") @RequestParam(required = false) Long sellerOrderId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ShipmentResponse> page = shipmentService.listShipments(status, sellerOrderId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Shipments retrieved successfully", page));
    }

    @GetMapping("/shipments/{id}")
    @Operation(summary = "Get shipment details", description = "Retrieves shipment details by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentById(
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long id) {
        ShipmentResponse response = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Shipment retrieved successfully", response));
    }

    @PostMapping("/assignments")
    @Operation(summary = "Assign delivery partner to shipment", description = "Manually assigns a shipment to an active, verified delivery partner")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Shipment assigned successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment or delivery partner not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Delivery partner unavailable or shipment already assigned", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> assignDeliveryBoy(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssignDeliveryBoyRequest request) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.adminAssignDeliveryBoy(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shipment assigned successfully", response));
    }

    @PostMapping("/assignments/{id}/reassign")
    @Operation(summary = "Reassign delivery partner", description = "Reassigns a shipment from the previous delivery partner to a new delivery partner")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment reassigned successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid reason", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment or new delivery partner not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "New delivery partner unavailable or invalid assignment state", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> reassignDeliveryBoy(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ReassignDeliveryRequest request) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.adminReassignDeliveryBoy(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Shipment reassigned successfully", response));
    }

    @GetMapping("/assignments")
    @Operation(summary = "List all assignments", description = "Retrieves all delivery assignments with optional filtering by delivery boy, shipment, and status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assignments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<DeliveryAssignmentResponse>>> listAssignments(
            @Parameter(description = "Filter by delivery partner ID") @RequestParam(required = false) Long deliveryBoyId,
            @Parameter(description = "Filter by shipment ID") @RequestParam(required = false) Long shipmentId,
            @Parameter(description = "Filter by assignment status") @RequestParam(required = false) DeliveryAssignmentStatus status,
            @PageableDefault(size = 10, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DeliveryAssignmentResponse> page = deliveryAssignmentService.adminListAssignments(
                deliveryBoyId, shipmentId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Assignments retrieved successfully", page));
    }

    @GetMapping("/tracking/{shipmentId}")
    @Operation(summary = "Get shipment tracking history", description = "Retrieves recorded GPS coordinate breadcrumb trail for a shipment")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<DeliveryTrackingResponse>>> getTrackingHistory(
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId,
            @PageableDefault(size = 50, sort = "recordedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DeliveryTrackingResponse> page = deliveryTrackingService.getLocationHistory(shipmentId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Tracking history retrieved successfully", page));
    }
}
