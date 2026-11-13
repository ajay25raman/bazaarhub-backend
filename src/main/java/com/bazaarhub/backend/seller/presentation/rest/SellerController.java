package com.bazaarhub.backend.seller.presentation.rest;

import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.seller.application.dto.request.SellerOnboardingRequest;
import com.bazaarhub.backend.seller.application.dto.request.UpdateSellerProfileRequest;
import com.bazaarhub.backend.seller.application.dto.response.SellerResponse;
import com.bazaarhub.backend.seller.application.dto.response.SellerStatusResponse;
import com.bazaarhub.backend.seller.application.service.SellerOnboardingService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sellers")
@Tag(name = "Seller Onboarding & Management", description = "Endpoints for merchant onboarding application, profile management, and status tracking")
@SecurityRequirement(name = "bearerAuth")
public class SellerController {

    private final SellerOnboardingService onboardingService;

    public SellerController(SellerOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/onboarding")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit seller onboarding application", description = "Submits a new seller application for the authenticated customer account. Initial verification and approval statuses are set to PENDING.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Seller application submitted successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on incoming business fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Seller application or profile already exists for this account")
    })
    public ResponseEntity<ApiResponse<SellerResponse>> submitOnboarding(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerOnboardingRequest request) {
        SellerResponse response = onboardingService.submitOnboarding(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Seller onboarding application submitted successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current seller profile", description = "Retrieves the authenticated merchant's seller profile, business contact details, and approval status.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller profile retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<SellerResponse>> getSellerProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        SellerResponse response = onboardingService.getSellerProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Seller profile retrieved successfully", response));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update seller business profile", description = "Updates permitted merchant business details. System-controlled approval and audit fields cannot be modified.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller profile updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on updated fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<SellerResponse>> updateSellerProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateSellerProfileRequest request) {
        SellerResponse response = onboardingService.updateSellerProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Seller profile updated successfully", response));
    }

    @GetMapping("/me/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get seller onboarding lifecycle status", description = "Fetches a quick overview of seller verification, approval, store readiness, and document completion status.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<SellerStatusResponse>> getSellerStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        SellerStatusResponse response = onboardingService.getSellerStatus(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Seller status retrieved successfully", response));
    }
}
