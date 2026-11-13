package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.catalog.application.dto.request.ProductVariantCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductVariantStatusRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductVariantUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.ProductVariantResponse;
import com.bazaarhub.backend.catalog.application.service.ProductVariantService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/products/{productId}/variants")
@Tag(name = "Seller Product Variants", description = "Endpoints for managing product SKU variants, pricing, attributes, and barcodes")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')")
public class SellerProductVariantController {

    private final ProductVariantService variantService;

    public SellerProductVariantController(ProductVariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping
    @Operation(summary = "Create product variant", description = "Adds a SKU variant to the product. Validates MRP >= selling price and unique SKU.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Variant created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or MRP < sellingPrice", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "SKU already exists", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductVariantResponse response = variantService.createVariant(principal.getId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Variant created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List product variants", description = "Retrieves all SKU variants for a product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Variants retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariants(@PathVariable Long productId) {
        List<ProductVariantResponse> variants = variantService.getVariants(productId);
        return ResponseEntity.ok(ApiResponse.ok(variants));
    }

    @GetMapping("/{variantId}")
    @Operation(summary = "Get variant by ID", description = "Retrieves details of a specific SKU variant.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Variant retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        ProductVariantResponse response = variantService.getVariant(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{variantId}")
    @Operation(summary = "Update product variant", description = "Updates SKU, barcode, pricing, weight, or attributes of an existing variant.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Variant updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or MRP < sellingPrice", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "SKU already in use", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductVariantResponse response = variantService.updateVariant(principal.getId(), productId, variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Variant updated successfully", response));
    }

    @PatchMapping("/{variantId}/status")
    @Operation(summary = "Toggle variant status", description = "Enables or disables an individual variant (ACTIVE / INACTIVE).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Variant status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariantStatus(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantStatusRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductVariantResponse response = variantService.updateVariantStatus(principal.getId(), productId, variantId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Variant status updated successfully", response));
    }

    @DeleteMapping("/{variantId}")
    @Operation(summary = "Delete product variant", description = "Deletes a variant from the product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Variant deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        variantService.deleteVariant(principal.getId(), productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Variant deleted successfully", null));
    }
}
