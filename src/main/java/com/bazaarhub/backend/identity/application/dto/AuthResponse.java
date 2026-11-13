package com.bazaarhub.backend.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing JWT access token and user profile")
public class AuthResponse {

    @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "HTTP authorization token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token time-to-live in milliseconds", example = "86400000")
    private long expiresIn;

    @Schema(description = "Safe authenticated user profile details")
    private UserProfileResponse user;
}
