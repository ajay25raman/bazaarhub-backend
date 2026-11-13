package com.bazaarhub.backend.seller.application.dto.response;

import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Seller profile response payload")
public class SellerResponse {

    @Schema(description = "Unique seller ID", example = "10")
    private Long id;

    @Schema(description = "Associated user ID", example = "101")
    private Long userId;

    @Schema(description = "Registered business name", example = "Acme Retail Enterprises")
    private String businessName;

    @Schema(description = "Business contact email", example = "contact@acmeretail.com")
    private String businessEmail;

    @Schema(description = "Business contact phone number", example = "+919876543210")
    private String businessPhone;

    @Schema(description = "Tax registration identifier", example = "29ABCDE1234F1Z5")
    private String taxIdentifier;

    @Schema(description = "KYC document verification status", example = "PENDING")
    private SellerVerificationStatus verificationStatus;

    @Schema(description = "Marketplace seller approval status", example = "PENDING")
    private SellerApprovalStatus approvalStatus;

    @Schema(description = "Timestamp when the seller was approved", example = "2026-11-13T12:00:00Z")
    private Instant approvedAt;

    @Schema(description = "Rejection feedback if application was declined", example = "null")
    private String rejectionReason;

    @Schema(description = "Storefront details if configured")
    private StoreResponse store;

    @Schema(description = "Application creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Profile last updated timestamp", example = "2026-11-13T10:30:00Z")
    private Instant updatedAt;
}
