package com.bazaarhub.backend.seller.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Seller settlement bank account submission payload")
public class SellerBankRequest {

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 150, message = "Account holder name must be between 2 and 150 characters")
    @Schema(description = "Full name of the bank account holder matching tax and KYC documents", example = "Acme Retail Enterprises Private Limited")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Bank account number must be between 9 and 18 digits")
    @Schema(description = "Bank account number (will be encrypted using AES-256-GCM; never stored or logged in plain text)", example = "50100012345678")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code must be a valid 11-character format (e.g., HDFC0001234)")
    @Schema(description = "Bank IFSC code (will be encrypted using AES-256-GCM)", example = "HDFC0001234")
    private String ifscCode;

    @Size(max = 150, message = "Bank name must not exceed 150 characters")
    @Schema(description = "Name of the banking institution", example = "HDFC Bank")
    private String bankName;

    @Size(max = 150, message = "Branch name must not exceed 150 characters")
    @Schema(description = "Branch name or location", example = "Indiranagar, Bangalore")
    private String branchName;

    @com.fasterxml.jackson.annotation.JsonProperty("isPrimary")
    @Schema(description = "Whether this account should be the primary settlement destination", example = "true")
    private boolean isPrimary;
}
