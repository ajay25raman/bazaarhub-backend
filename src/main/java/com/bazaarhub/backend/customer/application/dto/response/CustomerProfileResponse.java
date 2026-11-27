package com.bazaarhub.backend.customer.application.dto.response;

import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer profile and associated account details response")
public class CustomerProfileResponse {

    @Schema(description = "Profile ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "42")
    private Long userId;

    @Schema(description = "Customer full name", example = "Alice Johnson")
    private String fullName;

    @Schema(description = "Registered email address", example = "alice@example.com")
    private String email;

    @Schema(description = "Registered phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Date of birth", example = "1995-05-15")
    private LocalDate dateOfBirth;

    @Schema(description = "Gender", example = "FEMALE")
    private String gender;

    @Schema(description = "Profile image URL", example = "https://images.bazaarhub.com/profiles/alice.jpg")
    private String profileImageUrl;

    @Schema(description = "Account status", example = "ACTIVE")
    private AccountStatus accountStatus;

    @Schema(description = "Email verification status", example = "true")
    private boolean emailVerified;

    @Schema(description = "Phone verification status", example = "true")
    private boolean phoneVerified;

    @Schema(description = "Profile creation timestamp", example = "2026-03-01T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Profile last update timestamp", example = "2026-03-01T12:30:00Z")
    private Instant updatedAt;
}
