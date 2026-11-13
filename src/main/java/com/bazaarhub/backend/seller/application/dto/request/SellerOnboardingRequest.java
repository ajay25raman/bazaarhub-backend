package com.bazaarhub.backend.seller.application.dto.request;

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
@Schema(description = "Seller onboarding application submission payload")
public class SellerOnboardingRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    @Schema(description = "Registered legal business or trade name", example = "Acme Retail Enterprises")
    private String businessName;

    @Email(message = "Invalid business email format")
    @Size(max = 255, message = "Business email must not exceed 255 characters")
    @Schema(description = "Official business contact email", example = "contact@acmeretail.com")
    private String businessEmail;

    @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Business phone must be a valid phone number with 7 to 20 digits")
    @Schema(description = "Official business contact phone number", example = "+919876543210")
    private String businessPhone;

    @Size(max = 50, message = "Tax identifier must not exceed 50 characters")
    @Schema(description = "Tax identifier or registration number (e.g., GSTIN, PAN, VAT, EIN)", example = "29ABCDE1234F1Z5")
    private String taxIdentifier;
}
