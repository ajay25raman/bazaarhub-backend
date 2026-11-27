package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reject a delivery assignment")
public class RejectAssignmentRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(description = "Detailed reason for rejecting the task", example = "Outside serviceable operating zone", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
