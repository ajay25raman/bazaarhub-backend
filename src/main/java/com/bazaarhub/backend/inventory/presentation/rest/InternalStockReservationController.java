package com.bazaarhub.backend.inventory.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.inventory.application.dto.request.StockReserveRequest;
import com.bazaarhub.backend.inventory.application.dto.response.StockReservationResponse;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
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
@RequestMapping("/api/v1/internal/inventory/reservations")
@Tag(name = "Internal Stock Reservations", description = "Internal checkout service endpoints for locking, confirming, releasing, and expiring inventory reservations")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'INVENTORY_AUDIT')")
public class InternalStockReservationController {

    private final StockReservationService reservationService;

    public InternalStockReservationController(StockReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for checkout", description = "Locks stock for a unique checkout reference. Idempotent on repeated calls with matching quantity.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Stock reserved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient unreserved stock", content = @Content)
    })
    public ResponseEntity<ApiResponse<StockReservationResponse>> reserveStock(
            @Valid @RequestBody StockReserveRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        StockReservationResponse response = reservationService.reserveStock(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Stock reserved successfully", response));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm stock reservation", description = "Permanently deducts reserved stock and transitions status to CONFIRMED. Idempotent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservation confirmed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Reservation expired or invalid state", content = @Content)
    })
    public ResponseEntity<ApiResponse<StockReservationResponse>> confirmReservation(
            @RequestParam String checkoutReference,
            @RequestParam Long variantId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        StockReservationResponse response = reservationService.confirmReservation(checkoutReference, variantId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Reservation confirmed successfully", response));
    }

    @PostMapping("/release")
    @Operation(summary = "Release stock reservation", description = "Releases reserved stock back to available pool upon checkout cancellation or payment failure. Idempotent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservation released successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<StockReservationResponse>> releaseReservation(
            @RequestParam String checkoutReference,
            @RequestParam Long variantId,
            @RequestParam(required = false) String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        StockReservationResponse response = reservationService.releaseReservation(checkoutReference, variantId, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Reservation released successfully", response));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel stock reservation", description = "Transitions reservation to CANCELLED and returns reserved units to available stock. Idempotent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservation cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<StockReservationResponse>> cancelReservation(
            @RequestParam String checkoutReference,
            @RequestParam Long variantId,
            @RequestParam(required = false) String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        StockReservationResponse response = reservationService.cancelReservation(checkoutReference, variantId, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled successfully", response));
    }

    @PostMapping("/sweep-expired")
    @Operation(summary = "Sweep expired reservations", description = "Manually triggers reservation expiration sweep, releasing stock for all reservations whose TTL has elapsed.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sweep completed successfully")
    })
    public ResponseEntity<ApiResponse<Integer>> sweepExpiredReservations() {
        int count = reservationService.expirePendingReservations();
        return ResponseEntity.ok(ApiResponse.ok("Expired reservations sweep completed. Released count: " + count, count));
    }
}
