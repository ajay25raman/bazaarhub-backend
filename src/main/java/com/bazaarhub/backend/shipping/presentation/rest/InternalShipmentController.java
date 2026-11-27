package com.bazaarhub.backend.shipping.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.bazaarhub.backend.shipping.application.dto.request.CreateInternalShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ReadyForPickupRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ShippingRateQuoteRequest;
import com.bazaarhub.backend.shipping.application.dto.response.InternalShipmentResponse;
import com.bazaarhub.backend.shipping.application.dto.response.ShippingRateQuoteResponse;
import com.bazaarhub.backend.shipping.application.service.InternalShippingService;
import com.bazaarhub.backend.shipping.application.service.ShippingRateCalculator;
import com.bazaarhub.backend.shipping.domain.model.ShippingRateQuote;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipments")
@Tag(name = "Internal Shipping Operations", description = "Endpoints for creating and managing internal shipments, parcel preparation, and shipping rate estimation")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class InternalShipmentController {

    private final InternalShippingService internalShippingService;
    private final ShippingRateCalculator shippingRateCalculator;

    public InternalShipmentController(InternalShippingService internalShippingService,
                                      ShippingRateCalculator shippingRateCalculator) {
        this.internalShippingService = internalShippingService;
        this.shippingRateCalculator = shippingRateCalculator;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_SELLER', 'ROLE_ADMIN')")
    @Operation(summary = "Create shipment for seller sub-order", description = "Generates a unique internal shipment and tracking number for an eligible confirmed seller order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Shipment created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller order not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Shipment already exists for this order", content = @Content)
    })
    public ResponseEntity<ApiResponse<InternalShipmentResponse>> createShipment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateInternalShipmentRequest request) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        InternalShipmentResponse response = internalShippingService.createShipment(principal.getId(), request, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shipment created successfully", response));
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyAuthority('ORDER_READ', 'SELLER_ORDER_MANAGE', 'ROLE_CUSTOMER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    @Operation(summary = "Get shipment details by ID", description = "Retrieves full shipment details, addresses, items, and logistics status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Shipment belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InternalShipmentResponse>> getShipmentById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        InternalShipmentResponse response = internalShippingService.getShipmentById(principal.getId(), shipmentId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Shipment details retrieved successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ORDER_READ', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
    @Operation(summary = "Get shipments for customer order", description = "Retrieves all vendor-wise shipments associated with a multi-vendor customer order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order shipments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Order belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<InternalShipmentResponse>>> getShipmentsByOrderId(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Customer order ID", example = "1") @PathVariable Long orderId) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<InternalShipmentResponse> response = internalShippingService.getShipmentsByOrderId(principal.getId(), orderId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Order shipments retrieved successfully", response));
    }

    @PostMapping("/{shipmentId}/ready-for-pickup")
    @PreAuthorize("hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_SELLER', 'ROLE_ADMIN')")
    @Operation(summary = "Mark parcel ready for pickup", description = "Seller confirms parcel packaging and marks shipment status as READY_FOR_PICKUP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment marked ready for pickup",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Shipment belongs to another seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Shipment cannot transition to READY_FOR_PICKUP", content = @Content)
    })
    public ResponseEntity<ApiResponse<InternalShipmentResponse>> markReadyForPickup(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Shipment ID", example = "1") @PathVariable Long shipmentId,
            @Valid @RequestBody(required = false) ReadyForPickupRequest request) {
        InternalShipmentResponse response = internalShippingService.markReadyForPickup(principal.getId(), shipmentId, request);
        return ResponseEntity.ok(ApiResponse.ok("Shipment marked ready for delivery partner pickup", response));
    }

    @PostMapping("/rate-quote")
    @Operation(summary = "Calculate shipping rate quote", description = "Estimates shipping fee based on distance, weight, and order value without modifying database",
            security = {})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipping rate quote calculated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid rate quote request parameters", content = @Content)
    })
    public ResponseEntity<ApiResponse<ShippingRateQuoteResponse>> calculateRate(
            @Valid @RequestBody(required = false) ShippingRateQuoteRequest request) {
        ShippingRateQuote quote = (request != null && request.getSellerOrderId() != null)
                ? shippingRateCalculator.calculateRateForSellerOrder(request.getSellerOrderId())
                : shippingRateCalculator.calculateRate(request);

        ShippingRateQuoteResponse response = ShippingRateQuoteResponse.builder()
                .baseCharge(quote.getBaseCharge())
                .distanceKm(quote.getDistanceKm())
                .distanceCharge(quote.getDistanceCharge())
                .weightKg(quote.getWeightKg())
                .weightCharge(quote.getWeightCharge())
                .freeShippingApplied(quote.isFreeShippingApplied())
                .freeShippingThreshold(quote.getFreeShippingThreshold())
                .totalShippingCharge(quote.getTotalShippingCharge())
                .currency(quote.getCurrency())
                .calculationMethod(quote.getCalculationMethod())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Shipping rate calculated successfully", response));
    }
}
