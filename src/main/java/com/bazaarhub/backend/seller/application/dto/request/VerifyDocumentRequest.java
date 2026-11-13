package com.bazaarhub.backend.seller.application.dto.request;

import com.bazaarhub.backend.seller.domain.model.DocumentVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Administrative document review payload")
public class VerifyDocumentRequest {

    @NotNull(message = "Document ID is required")
    @Schema(description = "ID of the seller document under review", example = "42")
    private Long documentId;

    @NotNull(message = "Verification status is required")
    @Schema(description = "Target verification decision (VERIFIED or REJECTED)", example = "VERIFIED")
    private DocumentVerificationStatus status;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    @Schema(description = "Explanation if the document is rejected", example = "Document image is blurred and unreadable.")
    private String rejectionReason;
}
