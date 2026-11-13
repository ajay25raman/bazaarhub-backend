package com.bazaarhub.backend.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "User registration payload")
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    @Schema(description = "User's full legal or display name", example = "Ajay Kumar")
    private String fullName;

    @Email(message = "Email address must be well-formed")
    @Size(max = 255, message = "Email address must not exceed 255 characters")
    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be a valid E.164 or national format")
    @Schema(description = "User mobile number", example = "+919876543210")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(description = "User account password", example = "SecurePass123!")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Password confirmation matching the password field", example = "SecurePass123!")
    private String confirmPassword;
}
