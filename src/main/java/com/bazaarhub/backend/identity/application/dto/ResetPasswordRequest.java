package com.bazaarhub.backend.identity.application.dto;

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
@Schema(description = "Password reset completion payload")
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "One-time password reset token", example = "rst_token_123456")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    @Schema(description = "New secure password", example = "NewSecurePass123!")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Confirm new password", example = "NewSecurePass123!")
    private String confirmPassword;
}
