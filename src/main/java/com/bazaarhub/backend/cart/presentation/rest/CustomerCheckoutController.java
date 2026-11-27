package com.bazaarhub.backend.cart.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.cart.application.dto.request.CheckoutAddressRequest;
import com.bazaarhub.backend.cart.application.dto.request.CheckoutValidateRequest;
import com.bazaarhub.backend.cart.application.dto.response.CheckoutSummaryResponse;
import com.bazaarhub.backend.cart.application.dto.response.CheckoutValidationResponse;
import com.bazaarhub.backend.cart.application.service.CheckoutService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for checkout preparation and readiness validation:
 * Inspect checkout summary, validate prices and stock, and select delivery address.
 */
@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Customer Checkout Preparation", description = "Endpoints for authenticated customers to inspect checkout summary, validate items and stock, and set delivery address before order creation")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('CART_READ', 'CART_UPDATE', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerCheckoutController {

    private final CheckoutService checkoutService;

    public CustomerCheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get checkout summary", description = "Prepares and returns current checkout summary with real-time stock verification, price validation, multi-vendor seller grouping, and address status.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout summary generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Address belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<CheckoutSummaryResponse>> getCheckoutSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Optional Address ID to evaluate against checkout")
            @RequestParam(required = false) Long addressId) {
        CheckoutSummaryResponse response = checkoutService.getCheckoutSummary(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate checkout readiness", description = "Performs strict pre-order validation on all cart items, available stock, current pricing, and customer delivery address.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout validation evaluated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cart is empty or validation failed", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Address belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<CheckoutValidationResponse>> validateCheckout(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CheckoutValidateRequest request) {
        CheckoutValidationResponse response = checkoutService.validateCheckout(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/address")
    @Operation(summary = "Select delivery address for checkout", description = "Selects and verifies the customer delivery address for checkout, ensuring address ownership.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery address selected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on address selection", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Address belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<CheckoutSummaryResponse>> selectCheckoutAddress(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutAddressRequest request) {
        CheckoutSummaryResponse response = checkoutService.selectCheckoutAddress(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery address selected successfully", response));
    }
}
