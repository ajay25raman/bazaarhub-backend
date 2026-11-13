package com.bazaarhub.backend.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password recovery initiation payload")
public class ForgotPasswordRequest {

    @NotBlank(message = "Identifier (email or mobile) is required")
    @Schema(description = "Registered email address or mobile phone number", example = "user@example.com")
    private String identifier;
}
