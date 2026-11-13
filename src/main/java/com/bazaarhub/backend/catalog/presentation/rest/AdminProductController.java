package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.catalog.application.dto.request.ProductApprovalRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductStatusUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.ProductDetailResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductSummaryResponse;
import com.bazaarhub.backend.catalog.application.service.AdminProductService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@Tag(name = "Admin Product Moderation", description = "Administrative operations for reviewing, approving, rejecting, and governing marketplace products")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('PRODUCT_MODERATE', 'ROLE_ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    @Operation(summary = "List marketplace products for moderation", description = "Paginated product search with filtering by seller, category, brand, lifecycle status, approval status, and keyword.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin authority required", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductApprovalStatus approvalStatus,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductSummaryResponse> page = adminProductService.getProducts(sellerId, categoryId, brandId, status, approvalStatus, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product review details", description = "Retrieves complete product record including images, variants, seller info, and moderation history.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable Long productId) {
        ProductDetailResponse response = adminProductService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{productId}/approve")
    @Operation(summary = "Approve product", description = "Approves a pending product, clearing rejection reasons and publishing it to ACTIVE catalog status.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product approved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> approveProduct(
            @PathVariable Long productId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductDetailResponse response = adminProductService.approveProduct(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.ok("Product approved successfully", response));
    }

    @PostMapping("/{productId}/reject")
    @Operation(summary = "Reject product", description = "Rejects a product submission, setting approvalStatus to REJECTED and lifecycle status to INACTIVE. Requires explanation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product rejected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Rejection reason missing or invalid", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> rejectProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductApprovalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductDetailResponse response = adminProductService.rejectProduct(principal.getId(), productId, request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.ok("Product rejected successfully", response));
    }

    @PatchMapping("/{productId}/status")
    @Operation(summary = "Update product status as admin", description = "Allows administrators to suspend or toggle lifecycle status of any product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProductStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductDetailResponse response = adminProductService.updateProductStatus(principal.getId(), productId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Product status updated successfully", response));
    }
}
