package com.bazaarhub.backend.seller.application.dto.response;

import com.bazaarhub.backend.seller.domain.model.BankVerificationStatus;
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
@Schema(description = "Seller settlement bank account (safely masked representation)")
public class SellerBankResponse {

    @Schema(description = "Bank account record ID", example = "3")
    private Long id;

    @Schema(description = "Owner seller ID", example = "10")
    private Long sellerId;

    @Schema(description = "Account holder name", example = "Acme Retail Enterprises")
    private String accountHolderName;

    @Schema(description = "Masked account number (only last 4 digits visible)", example = "XXXXXXXX1234")
    private String maskedAccountNumber;

    @Schema(description = "Masked IFSC code", example = "HDFC***1234")
    private String maskedIfscCode;

    @Schema(description = "Bank name", example = "HDFC Bank")
    private String bankName;

    @Schema(description = "Branch name", example = "Indiranagar, Bangalore")
    private String branchName;

    @Schema(description = "Account verification status", example = "PENDING")
    private BankVerificationStatus verificationStatus;

    @com.fasterxml.jackson.annotation.JsonProperty("isPrimary")
    @Schema(description = "Whether this is the primary disbursement account", example = "true")
    private boolean isPrimary;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:20:00Z")
    private Instant createdAt;
}
