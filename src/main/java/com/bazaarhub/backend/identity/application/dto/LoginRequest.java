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
@Schema(description = "User authentication login payload")
public class LoginRequest {

    @NotBlank(message = "Login identifier (email or phone) is required")
    @Schema(description = "User's registered email address or phone number", example = "user@example.com")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Schema(description = "User account password", example = "SecurePass123!")
    private String password;

    @Schema(description = "Optional device registration/session metadata")
    private DeviceInfoRequest deviceInfo;
}
