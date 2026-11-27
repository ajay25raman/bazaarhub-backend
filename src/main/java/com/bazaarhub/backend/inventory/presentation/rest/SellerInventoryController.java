package com.bazaarhub.backend.inventory.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.inventory.application.dto.request.SafetyStockUpdateRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockAdjustmentRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockInRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockOutRequest;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryMovementResponse;
import com.bazaarhub.backend.inventory.application.dto.response.InventoryResponse;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
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
@RequestMapping("/api/v1/seller/inventory")
@Tag(name = "Seller Inventory", description = "Endpoints for approved sellers to manage SKU stock levels, thresholds, physical count adjustments, and audit movements")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('INVENTORY_UPDATE', 'INVENTORY_READ', 'ROLE_SELLER')")
public class SellerInventoryController {

    private final InventoryService inventoryService;

    public SellerInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List seller inventory", description = "Retrieves paginated stock levels for all product variants owned by the authenticated seller. Supports filtering by LOW_STOCK, OUT_OF_STOCK, and keyword search.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory list retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not an approved seller", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> listSellerInventory(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Page<InventoryResponse> page = inventoryService.getSellerInventories(principal.getId(), filter, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{variantId}")
    @Operation(summary = "Get variant inventory", description = "Retrieves current total, reserved, and available stock levels for a specific SKU variant owned by the seller.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Variant not owned by seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> getVariantInventory(
            @PathVariable Long variantId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        InventoryResponse response = inventoryService.getSellerVariantInventory(principal.getId(), variantId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{variantId}/safety-stock")
    @Operation(summary = "Update safety stock threshold", description = "Configures the minimum threshold quantity triggering low-stock alerts for the specified variant.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Safety stock updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or negative safety stock", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Variant not owned by seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> updateSafetyStock(
            @PathVariable Long variantId,
            @Valid @RequestBody SafetyStockUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        InventoryResponse response = inventoryService.updateSafetyStock(principal.getId(), variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Safety stock threshold updated successfully", response));
    }

    @PostMapping("/{variantId}/stock-in")
    @Operation(summary = "Stock replenishment (Stock In)", description = "Adds physical stock units to the variant. Creates an immutable STOCK_IN movement audit record.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock added successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Quantity must be greater than zero", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Variant not owned or product inactive/rejected", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> stockIn(
            @PathVariable Long variantId,
            @Valid @RequestBody StockInRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        InventoryResponse response = inventoryService.stockIn(principal.getId(), variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock replenished successfully", response));
    }

    @PostMapping("/{variantId}/stock-out")
    @Operation(summary = "Stock reduction (Stock Out)", description = "Reduces physical stock units for shrinkage, damage, or transfer. Prevents reduction below active reservations. Creates an immutable STOCK_OUT movement audit record.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Quantity must be greater than zero", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Variant not owned or product inactive/rejected", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient unreserved stock", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> stockOut(
            @PathVariable Long variantId,
            @Valid @RequestBody StockOutRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        InventoryResponse response = inventoryService.stockOut(principal.getId(), variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock reduced successfully", response));
    }

    @PostMapping("/{variantId}/adjustment")
    @Operation(summary = "Physical stock adjustment", description = "Reconciles variant stock count to match physical warehouse inventory. Mandatory reason required. Creates an immutable ADJUSTMENT movement audit record.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock adjusted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload, reason missing, or identical count", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Variant not owned or product inactive/rejected", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "New count is below currently reserved stock", content = @Content)
    })
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable Long variantId,
            @Valid @RequestBody StockAdjustmentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        InventoryResponse response = inventoryService.adjustStock(principal.getId(), variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock reconciled successfully", response));
    }

    @GetMapping("/{variantId}/movements")
    @Operation(summary = "View variant movement history", description = "Retrieves paginated audit log of all historical stock changes (STOCK_IN, STOCK_OUT, ADJUSTMENT, RESERVE, RELEASE) for this variant.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movements retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Variant not owned by seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<InventoryMovementResponse>>> getVariantMovements(
            @PathVariable Long variantId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Page<InventoryMovementResponse> movements = inventoryService.getSellerVariantMovements(principal.getId(), variantId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(movements));
    }
}
