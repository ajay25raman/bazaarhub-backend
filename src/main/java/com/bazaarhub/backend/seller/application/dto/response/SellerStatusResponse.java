package com.bazaarhub.backend.seller.application.dto.response;

import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
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
@Schema(description = "Seller onboarding and lifecycle status overview")
public class SellerStatusResponse {

    @Schema(description = "Seller profile ID", example = "10")
    private Long sellerId;

    @Schema(description = "Business name", example = "Acme Retail Enterprises")
    private String businessName;

    @Schema(description = "KYC document verification status", example = "PENDING")
    private SellerVerificationStatus verificationStatus;

    @Schema(description = "Platform approval status", example = "PENDING")
    private SellerApprovalStatus approvalStatus;

    @Schema(description = "Whether the seller has created a store", example = "true")
    private boolean hasStore;

    @Schema(description = "Status of the store, if created", example = "INACTIVE")
    private StoreStatus storeStatus;

    @Schema(description = "Number of KYC documents submitted", example = "2")
    private int documentsCount;

    @Schema(description = "Whether a settlement bank account has been submitted", example = "true")
    private boolean bankAccountConfigured;

    @Schema(description = "Whether the seller is fully active to trade", example = "false")
    private boolean canSell;

    @Schema(description = "Rejection feedback if application was rejected", example = "null")
    private String rejectionReason;

    @Schema(description = "Application submission timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Approval timestamp", example = "2026-11-13T12:00:00Z")
    private Instant approvedAt;
}
