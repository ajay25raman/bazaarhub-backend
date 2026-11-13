package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.catalog.application.dto.request.ProductCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductStatusUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.ProductDetailResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductSummaryResponse;
import com.bazaarhub.backend.catalog.application.service.ProductService;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
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

@RestController
@RequestMapping("/api/v1/seller/products")
@Tag(name = "Seller Product Management", description = "Endpoints for merchant product catalog lifecycle management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPDATE', 'ROLE_SELLER')")
public class SellerProductController {

    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product in the catalog. Initial approvalStatus is PENDING.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or inactive category/brand", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Approved seller required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product slug already exists", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductDetailResponse response = productService.createProduct(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created successfully and submitted for moderation", response));
    }

    @GetMapping
    @Operation(summary = "List seller's products", description = "Retrieves paginated products owned by the authenticated seller with optional filters.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getSellerProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductApprovalStatus approvalStatus,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Page<ProductSummaryResponse> page = productService.getSellerProducts(principal.getId(), status, approvalStatus, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get seller product by ID", description = "Retrieves detailed product representation with images and variants. Must be owned by seller.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getSellerProduct(
            @PathVariable Long productId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductDetailResponse response = productService.getSellerProduct(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update product details", description = "Updates title, description, category, brand, or slug of an owned product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid update payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Slug already in use", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductDetailResponse response = productService.updateProduct(principal.getId(), productId, request);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", response));
    }

    @PatchMapping("/{productId}/status")
    @Operation(summary = "Update product status", description = "Updates the lifecycle status of an owned product (e.g., DRAFT, ACTIVE, INACTIVE).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot activate rejected product", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProductStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductDetailResponse response = productService.updateProductStatus(principal.getId(), productId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Product status updated successfully", response));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Archive product", description = "Soft deletes/archives an owned product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product archived successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long productId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        productService.deleteProduct(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.ok("Product archived successfully", null));
    }
}
