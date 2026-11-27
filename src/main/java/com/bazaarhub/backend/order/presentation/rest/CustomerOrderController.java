package com.bazaarhub.backend.order.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.order.application.dto.request.CancelOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.CreateOrderRequest;
import com.bazaarhub.backend.order.application.dto.response.OrderResponse;
import com.bazaarhub.backend.order.application.dto.response.OrderSummaryResponse;
import com.bazaarhub.backend.order.application.service.OrderService;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for customer order operations:
 * Place order from cart, list orders, view order details, and cancel order.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Customer Orders", description = "Endpoints for authenticated customers to create, view, and cancel their orders")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('ORDER_CREATE', 'ORDER_READ', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place order from active cart", description = "Creates an order from the authenticated customer's shopping cart, reserves inventory, splits items by vendor, and converts the cart.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error, empty cart, or insufficient stock", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", response));
    }

    @GetMapping
    @Operation(summary = "List customer orders", description = "Retrieves paginated list of orders placed by the authenticated customer with optional status filter.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderSummaryResponse> orders = orderService.getCustomerOrders(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved successfully", orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details by ID", description = "Retrieves complete order tree including vendor sub-orders, order items, and audit history.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot access another customer's order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId) {
        OrderResponse response = orderService.getCustomerOrderById(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order details retrieved successfully", response));
    }

    @GetMapping("/by-number/{orderNumber}")
    @Operation(summary = "Get order details by order number", description = "Retrieves complete order tree using the unique order number reference.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot access another customer's order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderNumber) {
        OrderResponse response = orderService.getCustomerOrderByNumber(principal.getId(), orderNumber);
        return ResponseEntity.ok(ApiResponse.ok("Order details retrieved successfully", response));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", description = "Allows a customer to cancel an order in PENDING or CONFIRMED state. Releases reserved stock.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current state", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot cancel another customer's order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        OrderResponse response = orderService.cancelCustomerOrder(principal.getId(), orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully", response));
    }
}
