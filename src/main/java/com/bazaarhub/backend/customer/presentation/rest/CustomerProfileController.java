package com.bazaarhub.backend.customer.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.customer.application.dto.request.*;
import com.bazaarhub.backend.customer.application.dto.response.CustomerProfileResponse;
import com.bazaarhub.backend.customer.application.service.CustomerProfileService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/api/v1/customer/profile")
@Tag(name = "Customer Profile Management", description = "Endpoints for authenticated customers to view, initialize, and update personal account profile data")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUSTOMER_PROFILE_UPDATE', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerProfileController {

    private final CustomerProfileService profileService;

    public CustomerProfileController(CustomerProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get current customer profile", description = "Retrieves the personal profile details for the authenticated customer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse response = profileService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/account")
    @Operation(summary = "Get complete customer account profile", description = "Retrieves the full customer account information, returning basic user info even if extended profile is not yet created.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getAccountProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse response = profileService.getAccountProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @Operation(summary = "Create customer profile", description = "Initializes a customer profile if not already present. Prevents duplicate profile creation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Profile created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Profile already exists for customer", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> createProfile(
            @Valid @RequestBody CustomerProfileCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse response = profileService.createProfile(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Profile created successfully", response));
    }

    @PutMapping
    @Operation(summary = "Update customer profile", description = "Updates full personal profile information for the authenticated customer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
            @Valid @RequestBody CustomerProfileUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse response = profileService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/dob")
    @Operation(summary = "Update customer date of birth", description = "Updates only the date of birth field in the customer profile.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Date of birth updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date of birth", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateDateOfBirth(
            @Valid @RequestBody UpdateDobRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse response = profileService.updateDateOfBirth(principal.getId(), request.getDateOfBirth());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/gender")
    @Operation(summary = "Update customer gender", description = "Updates only the gender field in the customer profile.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Gender updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid gender value", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateGender(
            @Valid @RequestBody UpdateGenderRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse response = profileService.updateGender(principal.getId(), request.getGender());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/image")
    @Operation(summary = "Update customer profile image URL", description = "Updates only the profile image URL in the customer profile.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile image URL updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid image URL format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfileImageUrl(
            @Valid @RequestBody UpdateProfileImageRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse response = profileService.updateProfileImageUrl(principal.getId(), request.getProfileImageUrl());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
