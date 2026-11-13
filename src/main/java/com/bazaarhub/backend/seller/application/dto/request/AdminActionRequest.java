package com.bazaarhub.backend.seller.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Administrative seller review action payload (rejection/suspension reason)")
public class AdminActionRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(description = "Detailed justification or feedback for the rejection or suspension action", example = "KYC documents do not match tax registration number.")
    private String reason;
}
