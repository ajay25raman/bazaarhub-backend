package com.bazaarhub.backend.seller.presentation.rest;

import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.seller.application.dto.request.AdminActionRequest;
import com.bazaarhub.backend.seller.application.dto.request.VerifyDocumentRequest;
import com.bazaarhub.backend.seller.application.dto.response.*;
import com.bazaarhub.backend.seller.application.service.AdminSellerService;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/v1/admin/sellers")
@Tag(name = "Admin Seller Management", description = "Administrative endpoints for auditing seller applications, KYC documents, approval, rejection, and suspension")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('SELLER_MANAGE') or hasRole('ADMIN')")
public class AdminSellerController {

    private final AdminSellerService adminSellerService;

    public AdminSellerController(AdminSellerService adminSellerService) {
        this.adminSellerService = adminSellerService;
    }

    @GetMapping
    @Operation(summary = "List seller applications with filtering and pagination", description = "Retrieves a paginated list of seller applications filtered optionally by approval or verification status.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sellers retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: requires SELLER_MANAGE permission or ROLE_ADMIN")
    })
    public ResponseEntity<ApiResponse<Page<AdminSellerSummaryResponse>>> getSellers(
            @RequestParam(required = false) SellerApprovalStatus approvalStatus,
            @RequestParam(required = false) SellerVerificationStatus verificationStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AdminSellerSummaryResponse> page = adminSellerService.getSellers(approvalStatus, verificationStatus, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Seller applications retrieved successfully", page));
    }

    @GetMapping("/{sellerId}")
    @Operation(summary = "Get full seller application dossier", description = "Retrieves complete seller application details, user contact, storefront, submitted KYC documents, and settlement bank accounts.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: requires SELLER_MANAGE permission or ROLE_ADMIN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller not found")
    })
    public ResponseEntity<ApiResponse<AdminSellerDetailResponse>> getSellerDetails(
            @PathVariable Long sellerId) {
        AdminSellerDetailResponse response = adminSellerService.getSellerDetails(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller details retrieved successfully", response));
    }

    @PostMapping("/{sellerId}/approve")
    @Operation(summary = "Approve seller application", description = "Approves a pending seller application. Automatically assigns ROLE_SELLER to the merchant while preserving customer privileges, and activates their storefront.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller approved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid seller state or self-approval attempt"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: requires SELLER_MANAGE permission or ROLE_ADMIN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller not found")
    })
    public ResponseEntity<ApiResponse<SellerResponse>> approveSeller(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sellerId) {
        SellerResponse response = adminSellerService.approveSeller(principal.getId(), sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller approved successfully", response));
    }

    @PostMapping("/{sellerId}/reject")
    @Operation(summary = "Reject seller application", description = "Rejects a seller application. Requires a detailed rejection justification.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller rejected successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error: rejection reason is required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: requires SELLER_MANAGE permission or ROLE_ADMIN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller not found")
    })
    public ResponseEntity<ApiResponse<SellerResponse>> rejectSeller(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sellerId,
            @Valid @RequestBody AdminActionRequest request) {
        SellerResponse response = adminSellerService.rejectSeller(principal.getId(), sellerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seller application rejected", response));
    }

    @PostMapping("/{sellerId}/suspend")
    @Operation(summary = "Suspend seller account and storefront", description = "Suspends an active seller account and halts their storefront operations.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller suspended successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: requires SELLER_MANAGE permission or ROLE_ADMIN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller not found")
    })
    public ResponseEntity<ApiResponse<SellerResponse>> suspendSeller(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sellerId,
            @RequestBody(required = false) AdminActionRequest request) {
        SellerResponse response = adminSellerService.suspendSeller(principal.getId(), sellerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seller suspended successfully", response));
    }

    @PostMapping("/{sellerId}/verify-documents")
    @Operation(summary = "Review and verify KYC document", description = "Marks a specific seller KYC document as VERIFIED or REJECTED. When all documents are verified, the seller verification status automatically updates to VERIFIED.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Document verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on decision status or document ID"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: requires SELLER_MANAGE permission or ROLE_ADMIN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller or document not found")
    })
    public ResponseEntity<ApiResponse<SellerDocumentResponse>> verifyDocument(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sellerId,
            @Valid @RequestBody VerifyDocumentRequest request) {
        SellerDocumentResponse response = adminSellerService.verifyDocument(principal.getId(), sellerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Document reviewed successfully", response));
    }
}
