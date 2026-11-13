package com.bazaarhub.backend.catalog.application.dto.request;

import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
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
@Schema(description = "Admin product moderation review request")
public class ProductApprovalRequest {

    @NotNull(message = "Approval status is required")
    @Schema(description = "Product moderation verdict (APPROVED or REJECTED)", example = "APPROVED")
    private ProductApprovalStatus approvalStatus;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    @Schema(description = "Explanation for rejection (mandatory when status is REJECTED)", example = "Incomplete specification details and low-resolution image assets.")
    private String rejectionReason;
}
