package com.bazaarhub.backend.cart.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.cart.application.dto.request.AddToCartRequest;
import com.bazaarhub.backend.cart.application.dto.request.UpdateCartItemQuantityRequest;
import com.bazaarhub.backend.cart.application.dto.response.CartResponse;
import com.bazaarhub.backend.cart.application.dto.response.CartSummaryResponse;
import com.bazaarhub.backend.cart.application.service.CartService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for customer shopping cart operations:
 * View cart, add items, update quantities, remove items, clear cart, and fetch summary.
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Customer Cart Management", description = "Endpoints for authenticated customers to view, modify, and manage their multi-vendor shopping cart")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('CART_READ', 'CART_UPDATE', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerCartController {

    private final CartService cartService;

    public CustomerCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get current customer's cart", description = "Retrieves the active cart for the authenticated customer. Creates an active cart automatically if one does not exist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CartResponse cart = cartService.getCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product variant to the customer cart, validating product visibility, variant active status, and available warehouse stock.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Item added to cart successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid operation", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant or product not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient stock available in warehouse", content = @Content)
    })
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse cart = cartService.addToCart(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Item added to cart successfully", cart));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of an existing cart item after revalidating product active status and warehouse stock.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item quantity updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity or operation", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found in customer cart", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Requested quantity exceeds available stock", content = @Content)
    })
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        CartResponse cart = cartService.updateCartItemQuantity(principal.getId(), itemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart item quantity updated successfully", cart));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart", description = "Deletes a specific item from the customer's cart, strictly verifying item ownership.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Item belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId) {
        CartResponse cart = cartService.removeCartItem(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.ok("Cart item removed successfully", cart));
    }

    @DeleteMapping({"", "/items"})
    @Operation(summary = "Clear all items from cart", description = "Clears all items from the customer's active shopping cart in a single transaction.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CartResponse cart = cartService.clearCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared successfully", cart));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get cart summary", description = "Retrieves high-level cart summary including total items, unique items, subtotal, and seller-wise subtotal breakdown.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart summary retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<CartSummaryResponse>> getCartSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        CartSummaryResponse summary = cartService.getCartSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
