package com.bazaarhub.backend.seller.application.dto.response;

import com.bazaarhub.backend.seller.domain.model.DocumentVerificationStatus;
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
@Schema(description = "Seller KYC document representation")
public class SellerDocumentResponse {

    @Schema(description = "Unique document ID", example = "1")
    private Long id;

    @Schema(description = "Owner seller ID", example = "10")
    private Long sellerId;

    @Schema(description = "Type of document", example = "GST_CERTIFICATE")
    private String documentType;

    @Schema(description = "Document identification number", example = "29ABCDE1234F1Z5")
    private String documentNumber;

    @Schema(description = "Document storage URL", example = "https://storage.bazaarhub.com/kyc/acme-gst.pdf")
    private String documentUrl;

    @Schema(description = "Verification review status", example = "PENDING")
    private DocumentVerificationStatus verificationStatus;

    @Schema(description = "Feedback if document was rejected", example = "null")
    private String rejectionReason;

    @Schema(description = "Timestamp when the document was reviewed", example = "2026-11-13T11:00:00Z")
    private Instant reviewedAt;

    @Schema(description = "Document upload timestamp", example = "2026-11-13T10:15:00Z")
    private Instant createdAt;
}
