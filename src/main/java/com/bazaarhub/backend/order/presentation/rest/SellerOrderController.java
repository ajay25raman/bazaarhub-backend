package com.bazaarhub.backend.order.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.order.application.dto.request.CancelOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.UpdateOrderStatusRequest;
import com.bazaarhub.backend.order.application.dto.response.SellerOrderResponse;
import com.bazaarhub.backend.order.application.service.SellerOrderService;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
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

/**
 * REST controller for merchant sellers to manage sub-orders:
 * List sub-orders, view details, update fulfillment status, and cancel sub-orders.
 */
@RestController
@RequestMapping("/api/v1/seller/orders")
@Tag(name = "Seller Order Management", description = "Endpoints for authenticated merchant sellers to view and manage their vendor-specific sub-orders")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('SELLER_ORDER_MANAGE', 'ROLE_SELLER', 'ROLE_ADMIN')")
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    public SellerOrderController(SellerOrderService sellerOrderService) {
        this.sellerOrderService = sellerOrderService;
    }

    @GetMapping
    @Operation(summary = "List seller sub-orders", description = "Retrieves paginated list of sub-orders assigned to the authenticated merchant seller.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller orders retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not an approved seller", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<SellerOrderResponse>>> getSellerOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) SellerOrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SellerOrderResponse> orders = sellerOrderService.getSellerOrders(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Seller orders retrieved successfully", orders));
    }

    @GetMapping("/{sellerOrderId}")
    @Operation(summary = "Get seller sub-order by ID", description = "Retrieves sub-order details, ordered item lines, shipping destination, and fulfillment audit trail.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller order retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Sub-order does not belong to authenticated seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerOrderResponse>> getSellerOrderById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sellerOrderId) {
        SellerOrderResponse response = sellerOrderService.getSellerOrderById(principal.getId(), sellerOrderId);
        return ResponseEntity.ok(ApiResponse.ok("Seller order retrieved successfully", response));
    }

    @GetMapping("/by-number/{subOrderNumber}")
    @Operation(summary = "Get seller sub-order by sub-order number", description = "Retrieves sub-order details using the unique sub-order reference number.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller order retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Sub-order does not belong to authenticated seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerOrderResponse>> getSellerOrderBySubOrderNumber(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subOrderNumber) {
        SellerOrderResponse response = sellerOrderService.getSellerOrderBySubOrderNumber(principal.getId(), subOrderNumber);
        return ResponseEntity.ok(ApiResponse.ok("Seller order retrieved successfully", response));
    }

    @PatchMapping("/{sellerOrderId}/status")
    @Operation(summary = "Update seller sub-order fulfillment status", description = "Transitions sub-order status (e.g. CONFIRMED, PROCESSING, READY_FOR_PICKUP, SHIPPED, DELIVERED). Automatically synchronizes parent order status.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sub-order status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Sub-order does not belong to authenticated seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerOrderResponse>> updateSellerOrderStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sellerOrderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        SellerOrderResponse response = sellerOrderService.updateSellerOrderStatus(principal.getId(), sellerOrderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seller order status updated successfully", response));
    }

    @PostMapping("/{sellerOrderId}/cancel")
    @Operation(summary = "Cancel seller sub-order", description = "Allows a seller to cancel their sub-order prior to shipment. Restores stock back to warehouse inventory.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sub-order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Sub-order cannot be cancelled in its current state", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Sub-order does not belong to authenticated seller", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SellerOrderResponse>> cancelSellerOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sellerOrderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        SellerOrderResponse response = sellerOrderService.cancelSellerOrder(principal.getId(), sellerOrderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seller order cancelled successfully", response));
    }
}
