package com.bazaarhub.backend.customer.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer profile initial creation request")
public class CustomerProfileCreateRequest {

    @Schema(description = "Optional updated full name", example = "Alice Johnson")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @Schema(description = "Date of birth (must be in the past)", example = "1995-05-15")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Schema(description = "Gender", example = "FEMALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    @Pattern(regexp = "(?i)^(MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY)$", message = "Gender must be MALE, FEMALE, OTHER, or PREFER_NOT_TO_SAY")
    private String gender;

    @Schema(description = "Profile image URL", example = "https://images.bazaarhub.com/profiles/alice.jpg")
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    @URL(message = "Profile image URL must be a valid URL")
    private String profileImageUrl;
}
