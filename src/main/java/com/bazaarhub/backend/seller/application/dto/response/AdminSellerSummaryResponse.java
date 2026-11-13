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
@Schema(description = "Paginated administrative seller application summary")
public class AdminSellerSummaryResponse {

    @Schema(description = "Seller profile ID", example = "10")
    private Long id;

    @Schema(description = "User ID", example = "101")
    private Long userId;

    @Schema(description = "Registered business name", example = "Acme Retail Enterprises")
    private String businessName;

    @Schema(description = "Business email", example = "contact@acmeretail.com")
    private String businessEmail;

    @Schema(description = "Tax identifier", example = "29ABCDE1234F1Z5")
    private String taxIdentifier;

    @Schema(description = "Verification status", example = "PENDING")
    private SellerVerificationStatus verificationStatus;

    @Schema(description = "Approval status", example = "PENDING")
    private SellerApprovalStatus approvalStatus;

    @Schema(description = "Store name if created", example = "Acme Official Store")
    private String storeName;

    @Schema(description = "Application timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;
}
