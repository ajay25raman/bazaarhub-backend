package com.bazaarhub.backend.seller.application.dto.response;

import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed seller application dossier for administrator audit and approval")
public class AdminSellerDetailResponse {

    @Schema(description = "Seller profile ID", example = "10")
    private Long id;

    @Schema(description = "Underlying customer user ID", example = "101")
    private Long userId;

    @Schema(description = "User full legal name", example = "Alice Wonderland")
    private String userFullName;

    @Schema(description = "User account email", example = "alice@example.com")
    private String userEmail;

    @Schema(description = "User account phone", example = "+919123456789")
    private String userPhone;

    @Schema(description = "Registered business name", example = "Acme Retail Enterprises")
    private String businessName;

    @Schema(description = "Business email", example = "contact@acmeretail.com")
    private String businessEmail;

    @Schema(description = "Business phone", example = "+919876543210")
    private String businessPhone;

    @Schema(description = "Tax registration identifier", example = "29ABCDE1234F1Z5")
    private String taxIdentifier;

    @Schema(description = "KYC document verification status", example = "PENDING")
    private SellerVerificationStatus verificationStatus;

    @Schema(description = "Marketplace approval status", example = "PENDING")
    private SellerApprovalStatus approvalStatus;

    @Schema(description = "Admin user ID who approved/reviewed the seller", example = "1")
    private Long approvedById;

    @Schema(description = "Approval timestamp", example = "2026-11-13T12:00:00Z")
    private Instant approvedAt;

    @Schema(description = "Rejection feedback", example = "null")
    private String rejectionReason;

    @Schema(description = "Storefront details if created")
    private StoreResponse store;

    @Schema(description = "Submitted KYC verification documents")
    private List<SellerDocumentResponse> documents;

    @Schema(description = "Submitted settlement bank accounts (safely masked)")
    private List<SellerBankResponse> bankAccounts;

    @Schema(description = "Application creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Profile last updated timestamp", example = "2026-11-13T10:30:00Z")
    private Instant updatedAt;
}
