package com.bazaarhub.backend.seller.presentation.rest;

import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.seller.application.dto.request.StoreCreateRequest;
import com.bazaarhub.backend.seller.application.dto.request.StoreUpdateRequest;
import com.bazaarhub.backend.seller.application.dto.response.StoreResponse;
import com.bazaarhub.backend.seller.application.service.StoreService;
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
@RequestMapping("/api/v1/sellers/me/store")
@Tag(name = "Storefront Management", description = "Endpoints for creating and configuring the merchant's storefront")
@SecurityRequirement(name = "bearerAuth")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create merchant storefront", description = "Creates a new storefront for the authenticated seller. Initial status is INACTIVE and activates upon administrator approval.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Storefront created successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on store name or slug"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Store already exists for this seller or slug collision")
    })
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoreCreateRequest request) {
        StoreResponse response = storeService.createStore(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Storefront created successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get merchant storefront", description = "Retrieves the storefront details configured for the authenticated seller.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Storefront retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No storefront configured for this seller")
    })
    public ResponseEntity<ApiResponse<StoreResponse>> getMyStore(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        StoreResponse response = storeService.getMyStore(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Storefront retrieved successfully", response));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update merchant storefront", description = "Updates permitted storefront fields (name, description, logo, banner). Store activation status remains administrator-controlled.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Storefront updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on updated fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No storefront configured for this seller")
    })
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoreUpdateRequest request) {
        StoreResponse response = storeService.updateStore(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Storefront updated successfully", response));
    }
}
