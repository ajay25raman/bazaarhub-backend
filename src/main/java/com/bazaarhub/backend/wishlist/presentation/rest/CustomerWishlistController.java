package com.bazaarhub.backend.wishlist.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.bazaarhub.backend.wishlist.application.dto.request.AddWishlistItemRequest;
import com.bazaarhub.backend.wishlist.application.dto.request.WishlistUpdateRequest;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistItemResponse;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistProductExistsResponse;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistResponse;
import com.bazaarhub.backend.wishlist.application.service.CustomerWishlistService;
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

@RestController
@RequestMapping("/api/v1/customer/wishlist")
@Tag(name = "Customer Wishlist Management", description = "Endpoints for authenticated customers to save products for later, organize wishlist, and manage saved items")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('WISHLIST_READ', 'WISHLIST_UPDATE', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerWishlistController {

    private final CustomerWishlistService wishlistService;

    public CustomerWishlistController(CustomerWishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @Operation(summary = "Get customer wishlist", description = "Retrieves the authenticated customer's wishlist metadata. Automatically creates a default wishlist if one does not exist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        WishlistResponse response = wishlistService.getCustomerWishlist(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    @Operation(summary = "Rename customer wishlist", description = "Updates the display name of the authenticated customer's wishlist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist renamed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on wishlist name", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<WishlistResponse>> updateWishlistName(
            @Valid @RequestBody WishlistUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        WishlistResponse response = wishlistService.updateWishlistName(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/items")
    @Operation(summary = "Get wishlist items", description = "Retrieves paginated product items saved in the customer's wishlist, ordered by most recently added.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist items retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<WishlistItemResponse>>> getWishlistItems(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Page<WishlistItemResponse> items = wishlistService.getWishlistItems(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PostMapping("/items")
    @Operation(summary = "Add product to wishlist", description = "Adds an eligible, active catalog product to the customer's wishlist. Prevents duplicate entries safely under concurrency.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product added to wishlist successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Product is inactive or ineligible for listing", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product is already present in wishlist", content = @Content)
    })
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addProductToWishlist(
            @Valid @RequestBody AddWishlistItemRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        WishlistItemResponse response = wishlistService.addProductToWishlist(principal.getId(), request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Product added to wishlist successfully", response));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove product from wishlist", description = "Removes a specific product from the authenticated customer's wishlist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product removed from wishlist successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product is not present in wishlist", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> removeProductFromWishlist(
            @PathVariable Long productId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        wishlistService.removeProductFromWishlist(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.ok("Product removed from wishlist successfully", null));
    }

    @GetMapping("/items/{productId}/exists")
    @Operation(summary = "Check if product is in wishlist", description = "Checks whether a specific catalog product is currently in the authenticated customer's wishlist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist status checked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<WishlistProductExistsResponse>> checkProductInWishlist(
            @PathVariable Long productId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        WishlistProductExistsResponse response = wishlistService.checkProductInWishlist(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Clear customer wishlist", description = "Removes all saved products from the authenticated customer's wishlist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist cleared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> clearWishlist(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        wishlistService.clearWishlist(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Wishlist cleared successfully", null));
    }
}
