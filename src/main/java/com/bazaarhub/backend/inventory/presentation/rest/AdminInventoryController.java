package com.bazaarhub.backend.inventory.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.inventory.application.dto.request.SafetyStockUpdateRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockAdjustmentRequest;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryMovementResponse;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryResponse;
import com.bazaarhub.backend.inventory.application.dto.response.StockReservationResponse;
import com.bazaarhub.backend.inventory.application.service.AdminInventoryService;
import com.bazaarhub.backend.inventory.domain.model.ReservationStatus;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/api/v1/admin/inventory")
@Tag(name = "Admin Inventory", description = "Endpoints for platform administrators to oversee marketplace inventory, audit discrepancies, adjust counts, and inspect reservations")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('INVENTORY_AUDIT', 'ROLE_ADMIN')")
public class AdminInventoryController {

    private final AdminInventoryService adminInventoryService;

    public AdminInventoryController(AdminInventoryService adminInventoryService) {
        this.adminInventoryService = adminInventoryService;
    }

    @GetMapping
    @Operation(summary = "List all platform inventory", description = "Retrieves paginated inventory across all vendors. Supports filtering by sellerId, productId, LOW_STOCK, OUT_OF_STOCK, and keyword search.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory page retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires administrative authority", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> listAllInventory(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InventoryResponse> page = adminInventoryService.getAllInventory(filter, sellerId, productId, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List low-stock inventory", description = "Convenience filter returning all variants whose available stock is less than or equal to their safety stock threshold.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Low-stock items retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getLowStock(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InventoryResponse> page = adminInventoryService.getAllInventory("LOW_STOCK", sellerId, null, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/out-of-stock")
    @Operation(summary = "List out-of-stock inventory", description = "Convenience filter returning all variants whose available stock is zero or depleted.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Out-of-stock items retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getOutOfStock(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InventoryResponse> page = adminInventoryService.getAllInventory("OUT_OF_STOCK", sellerId, null, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{variantId}")
    @Operation(summary = "Get inventory by variant ID", description = "Inspects stock levels, reserved quantities, and safety threshold for a specific SKU variant.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> getVariantInventory(@PathVariable Long variantId) {
        InventoryResponse response = adminInventoryService.getInventoryByVariantId(variantId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{variantId}/adjustment")
    @Operation(summary = "Authoritative stock adjustment", description = "Adjusts SKU variant stock count. Mandatory documented justification required. Creates an immutable ADJUSTMENT movement record recording the administrator.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock adjusted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or identical count", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "New count is below currently reserved stock", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable Long variantId,
            @Valid @RequestBody StockAdjustmentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        InventoryResponse response = adminInventoryService.adminAdjustStock(principal.getId(), variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock count adjusted by administrator", response));
    }

    @PatchMapping("/{variantId}/safety-stock")
    @Operation(summary = "Update safety stock threshold", description = "Sets the safety threshold triggering low-stock alerts for the specified variant.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Safety stock updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Negative threshold supplied", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> updateSafetyStock(
            @PathVariable Long variantId,
            @Valid @RequestBody SafetyStockUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        InventoryResponse response = adminInventoryService.adminUpdateSafetyStock(principal.getId(), variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Safety stock updated successfully", response));
    }

    @GetMapping("/{variantId}/movements")
    @Operation(summary = "View variant movement history", description = "Retrieves paginated audit trail of all historical stock movements for the SKU variant.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movements retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InventoryMovementResponse>>> getVariantMovements(
            @PathVariable Long variantId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InventoryMovementResponse> movements = adminInventoryService.getMovementsForVariant(variantId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(movements));
    }

    @GetMapping("/reservations")
    @Operation(summary = "List stock reservations", description = "Inspects active, confirmed, released, or expired checkout reservations across the platform.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservations retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<StockReservationResponse>>> listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StockReservationResponse> reservations = adminInventoryService.getReservations(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(reservations));
    }
}
