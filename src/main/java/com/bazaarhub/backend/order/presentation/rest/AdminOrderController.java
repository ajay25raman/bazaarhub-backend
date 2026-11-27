package com.bazaarhub.backend.order.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.order.application.dto.request.UpdateOrderStatusRequest;
import com.bazaarhub.backend.order.application.dto.response.OrderResponse;
import com.bazaarhub.backend.order.application.dto.response.OrderSummaryResponse;
import com.bazaarhub.backend.order.application.dto.response.OrderStatusHistoryResponse;
import com.bazaarhub.backend.order.application.dto.response.SellerOrderResponse;
import com.bazaarhub.backend.order.application.service.AdminOrderService;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
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

import java.util.List;

/**
 * REST controller for administrative order management:
 * System-wide order queries, multi-vendor oversight, status overrides, and audit trails.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin Order Management", description = "Endpoints for administrators to inspect, filter, override, and audit orders across all marketplace participants")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('ADMIN_ORDER_MANAGE', 'ROLE_ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    @Operation(summary = "Search and list all orders", description = "Retrieves paginated list of all marketplace orders with optional filters by status and customer ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderSummaryResponse> orders = adminOrderService.getAllOrders(status, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved successfully", orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get complete order tree", description = "Retrieves full details of an order including all vendor sub-orders, items, shipping address, and audit history.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long orderId) {
        OrderResponse response = adminOrderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order retrieved successfully", response));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Administratively update order status", description = "Updates order status with administrative audit remarks. Cascades cancellation and stock release if marked CANCELLED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponse response = adminOrderService.updateOrderStatus(orderId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Order status updated successfully", response));
    }

    @GetMapping("/seller-orders")
    @Operation(summary = "List all seller sub-orders", description = "Retrieves paginated list of vendor sub-orders across all marketplace merchants with optional filters.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller orders retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<SellerOrderResponse>>> getAllSellerOrders(
            @RequestParam(required = false) SellerOrderStatus status,
            @RequestParam(required = false) Long sellerId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SellerOrderResponse> response = adminOrderService.getAllSellerOrders(status, sellerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Seller orders retrieved successfully", response));
    }

    @GetMapping("/{orderId}/history")
    @Operation(summary = "Get order audit history", description = "Retrieves full chronology of status transitions recorded for an order.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order history retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderHistory(@PathVariable Long orderId) {
        List<OrderStatusHistoryResponse> history = adminOrderService.getOrderHistory(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order history retrieved successfully", history));
    }
}
