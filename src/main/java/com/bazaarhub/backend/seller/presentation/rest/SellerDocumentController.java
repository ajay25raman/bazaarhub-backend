package com.bazaarhub.backend.seller.presentation.rest;

import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.seller.application.dto.request.SellerDocumentRequest;
import com.bazaarhub.backend.seller.application.dto.response.SellerDocumentResponse;
import com.bazaarhub.backend.seller.application.service.SellerDocumentService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/sellers/me/documents")
@Tag(name = "Seller KYC Documents", description = "Endpoints for submitting and managing merchant verification documents")
@SecurityRequirement(name = "bearerAuth")
public class SellerDocumentController {

    private final SellerDocumentService documentService;

    public SellerDocumentController(SellerDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit KYC verification document", description = "Submits a verification document (e.g., GST Certificate, PAN, Business License) for compliance review. Initial status is PENDING.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Document submitted successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on document type or URL"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<SellerDocumentResponse>> uploadDocument(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerDocumentRequest request) {
        SellerDocumentResponse response = documentService.uploadDocument(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("KYC document submitted successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List seller KYC documents", description = "Retrieves all KYC verification documents submitted by the authenticated merchant.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<List<SellerDocumentResponse>>> getDocuments(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        List<SellerDocumentResponse> documents = documentService.getDocuments(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Seller documents retrieved successfully", documents));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get seller KYC document by ID", description = "Retrieves a single KYC document. Enforces strict tenant ownership: merchants cannot view documents of other sellers.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Document retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: document belongs to another merchant"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<ApiResponse<SellerDocumentResponse>> getDocumentById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long documentId) {
        SellerDocumentResponse response = documentService.getDocumentById(principal.getId(), documentId);
        return ResponseEntity.ok(ApiResponse.ok("Document retrieved successfully", response));
    }
}
