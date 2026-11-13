package com.bazaarhub.backend.identity.application.dto;

import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Safe user profile representation")
public class UserProfileResponse {

    @Schema(description = "Unique user ID", example = "101")
    private Long id;

    @Schema(description = "Full name", example = "Ajay Kumar")
    private String fullName;

    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @Schema(description = "Mobile phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Current account status", example = "ACTIVE")
    private AccountStatus accountStatus;

    @Schema(description = "Whether email is verified", example = "true")
    private boolean emailVerified;

    @Schema(description = "Whether phone number is verified", example = "false")
    private boolean phoneVerified;

    @Schema(description = "Assigned security roles", example = "[\"ROLE_CUSTOMER\"]")
    private Set<String> roles;

    @Schema(description = "Assigned granular permissions", example = "[\"PRODUCT_READ\"]")
    private Set<String> permissions;

    @Schema(description = "Last login timestamp", example = "2026-11-13T10:00:00Z")
    private Instant lastLoginAt;

    @Schema(description = "Account registration timestamp", example = "2026-11-13T09:00:00Z")
    private Instant createdAt;
}
