package com.bazaarhub.backend.seller.presentation.rest;

import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.seller.application.dto.request.SellerBankRequest;
import com.bazaarhub.backend.seller.application.dto.response.SellerBankResponse;
import com.bazaarhub.backend.seller.application.service.SellerBankService;
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
@RequestMapping("/api/v1/sellers/me/bank")
@Tag(name = "Seller Settlement Bank Accounts", description = "Endpoints for managing settlement bank accounts. Banking data is encrypted at rest using AES-256-GCM and responses return masked details only.")
@SecurityRequirement(name = "bearerAuth")
public class SellerBankController {

    private final SellerBankService bankService;

    public SellerBankController(SellerBankService bankService) {
        this.bankService = bankService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit settlement bank account", description = "Submits a bank account for seller payouts. Sensitive account number and IFSC are encrypted using AES-256-GCM. Response returns masked details only.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bank account saved and encrypted successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on bank details or IFSC format"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<SellerBankResponse>> addBankAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerBankRequest request) {
        SellerBankResponse response = bankService.addBankAccount(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bank account registered and encrypted successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List seller bank accounts", description = "Retrieves all configured settlement bank accounts for the authenticated seller with masked account numbers.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank accounts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<List<SellerBankResponse>>> getBankAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        List<SellerBankResponse> accounts = bankService.getBankAccounts(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Bank accounts retrieved successfully", accounts));
    }

    @GetMapping("/{bankId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get seller bank account by ID", description = "Retrieves a single settlement bank account with masked numbers. Enforces ownership: sellers cannot access accounts of other merchants.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank account retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: account belongs to another merchant"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bank account not found")
    })
    public ResponseEntity<ApiResponse<SellerBankResponse>> getBankAccountById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bankId) {
        SellerBankResponse response = bankService.getBankAccountById(principal.getId(), bankId);
        return ResponseEntity.ok(ApiResponse.ok("Bank account retrieved successfully", response));
    }
}
