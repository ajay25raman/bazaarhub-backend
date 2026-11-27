package com.bazaarhub.backend.delivery.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.dto.response.*;
import com.bazaarhub.backend.delivery.application.service.DeliveryAssignmentService;
import com.bazaarhub.backend.delivery.application.service.DeliveryBoyService;
import com.bazaarhub.backend.delivery.application.service.DeliveryTrackingService;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
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

import java.time.Instant;
import java.util.Map;

/**
 * REST controller exposing endpoints for Delivery Partners:
 * Onboarding, profile management, availability toggling, assignment lifecycle workflow,
 * OTP completion, COD collection, and GPS tracking.
 */
@RestController
@RequestMapping("/api/v1/delivery")
@Tag(name = "Delivery Partner Operations", description = "Endpoints for delivery partners to manage profiles, accept deliveries, verify OTPs, and submit GPS tracking")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DeliveryBoyController {

    private final DeliveryBoyService deliveryBoyService;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final DeliveryTrackingService deliveryTrackingService;
    private final CloudinaryService cloudinaryService;

    public DeliveryBoyController(DeliveryBoyService deliveryBoyService,
                                 DeliveryAssignmentService deliveryAssignmentService,
                                 DeliveryTrackingService deliveryTrackingService,
                                 CloudinaryService cloudinaryService) {
        this.deliveryBoyService = deliveryBoyService;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.deliveryTrackingService = deliveryTrackingService;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/onboard")
    @Operation(summary = "Onboard as delivery partner", description = "Registers authenticated user as a delivery partner and assigns ROLE_DELIVERY")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Delivery partner registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error in onboarding payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User is already registered as a delivery partner", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> onboard(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeliveryBoyOnboardRequest request) {
        DeliveryBoyResponse response = deliveryBoyService.onboard(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Delivery partner registered successfully", response));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Get delivery partner profile", description = "Retrieves logged-in delivery partner's profile and operational status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY or ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery partner record not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryBoyResponse response = deliveryBoyService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully", response));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Update delivery partner profile", description = "Updates vehicle details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY or ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery partner not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeliveryBoyUpdateRequest request) {
        DeliveryBoyResponse response = deliveryBoyService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", response));
    }

    @PatchMapping("/availability")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Toggle availability status", description = "Switches status between AVAILABLE and OFFLINE")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY or ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery partner not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> updateAvailability(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        DeliveryBoyResponse response = deliveryBoyService.updateAvailability(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Availability updated successfully", response));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "List assigned deliveries", description = "Retrieves paginated deliveries assigned to the delivery partner")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assignments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<DeliveryAssignmentResponse>>> getAssignments(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by assignment status") @RequestParam(required = false) DeliveryAssignmentStatus status,
            @PageableDefault(size = 10, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DeliveryAssignmentResponse> page = deliveryAssignmentService.getDeliveryBoyAssignments(
                principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Assignments retrieved successfully", page));
    }

    @GetMapping("/assignments/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Get delivery assignment details", description = "Retrieves shipment details, pickup address, delivery address, items, and COD amount")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assignment retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Assignment belongs to another partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> getAssignmentById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.getAssignmentById(principal.getId(), id, false);
        return ResponseEntity.ok(ApiResponse.ok("Assignment retrieved successfully", response));
    }

    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Accept delivery assignment", description = "Accepts an assigned delivery task")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assignment accepted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Assignment cannot be accepted from current status", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> acceptAssignment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.acceptAssignment(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Assignment accepted successfully", response));
    }

    @PostMapping("/assignments/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Reject delivery assignment", description = "Rejects an assigned delivery task with a mandatory reason")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assignment rejected successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or missing reason", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Assignment cannot be rejected from current status", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> rejectAssignment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectAssignmentRequest request) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.rejectAssignment(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Assignment rejected", response));
    }

    @PostMapping("/assignments/{id}/pickup")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Confirm parcel pickup", description = "Confirms package collected from seller store and transitions status to PICKED_UP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Package pickup confirmed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Package cannot be picked up in current status", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> confirmPickup(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody(required = false) PickupConfirmationRequest request) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.confirmPickup(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Package pickup confirmed successfully", response));
    }

    @PostMapping("/assignments/{id}/out-for-delivery")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Mark out for delivery", description = "Transitions shipment to OUT_FOR_DELIVERY and triggers OTP dispatch to customer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment marked out for delivery; OTP dispatched",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Shipment cannot transition to OUT_FOR_DELIVERY", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> markOutForDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.markOutForDelivery(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Shipment is out for delivery. OTP dispatched to customer.", response));
    }

    @PostMapping("/assignments/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Complete delivery with OTP", description = "Verifies customer OTP, records proof, settles COD cash collection if applicable, and completes delivery")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery verified and completed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid OTP, missing proof, or invalid COD collection", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Delivery cannot be completed from current status", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> completeDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody CompleteDeliveryRequest request) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.completeDelivery(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery verified and completed successfully", response));
    }

    @PostMapping("/assignments/{id}/fail")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Report delivery failure", description = "Marks delivery attempt as failed with business reason and notes")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery failure recorded",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or missing reason", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Invalid status transition for failure", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> failDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Assignment ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody FailedDeliveryRequest request) {
        DeliveryAssignmentResponse response = deliveryAssignmentService.failDelivery(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery failure recorded", response));
    }

    @PostMapping("/tracking/location")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Update live GPS location", description = "Submits current coordinates for an active transit shipment")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Location recorded successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid coordinate format or values", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Active shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<DeliveryTrackingResponse>> updateLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateLocationRequest request) {
        DeliveryTrackingResponse response = deliveryTrackingService.recordLocation(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Location recorded successfully", response));
    }

    @GetMapping("/route-estimate/{shipmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Get route distance and duration estimate", description = "Calculates route distance and duration to delivery address via Google Maps")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Route estimate calculated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not assigned to this partner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<RouteEstimateResponse>> getRouteEstimate(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = principal != null ? principal.getId() : null;

        RouteEstimateResponse response = deliveryTrackingService.getRouteEstimate(userId, shipmentId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Route estimate calculated", response));
    }

    @PostMapping("/proof/upload")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
    @Operation(summary = "Upload delivery proof image", description = "Uploads proof of delivery or failed delivery evidence to Cloudinary")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proof uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid image data", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Cloudinary upload failure", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProofUploadResponse>> uploadProof(
            @Valid @RequestBody DeliveryProofUploadRequest request) {
        String image = request.getImage();
        String folder = (request.getFolder() != null && !request.getFolder().isBlank())
                ? request.getFolder()
                : "bazaarhub/delivery/proofs";
        String url = cloudinaryService.uploadImage(image, folder);
        ProofUploadResponse response = ProofUploadResponse.builder()
                .url(url)
                .folder(folder)
                .uploadedAt(Instant.now())
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Proof uploaded successfully", response));
    }
}
