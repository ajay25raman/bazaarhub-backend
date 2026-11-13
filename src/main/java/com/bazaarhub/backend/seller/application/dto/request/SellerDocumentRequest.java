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
@Schema(description = "Seller KYC document submission payload")
public class SellerDocumentRequest {

    @NotBlank(message = "Document type is required")
    @Size(max = 50, message = "Document type must not exceed 50 characters")
    @Schema(description = "Type of verification document (e.g., GST_CERTIFICATE, PAN_CARD, BUSINESS_LICENSE, IDENTITY_PROOF)", example = "GST_CERTIFICATE")
    private String documentType;

    @Size(max = 100, message = "Document number must not exceed 100 characters")
    @Schema(description = "Document identification number or certificate code", example = "GSTIN29ABCDE1234F1Z5")
    private String documentNumber;

    @NotBlank(message = "Document URL is required")
    @Size(max = 500, message = "Document URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://).+", message = "Document URL must be a valid HTTP or HTTPS URL")
    @Schema(description = "Secure accessible URL of the uploaded verification document", example = "https://storage.bazaarhub.com/kyc/acme-gst-certificate.pdf")
    private String documentUrl;
}
