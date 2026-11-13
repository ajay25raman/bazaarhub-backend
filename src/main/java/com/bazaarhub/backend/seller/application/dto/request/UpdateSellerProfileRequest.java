package com.bazaarhub.backend.seller.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Seller profile update payload (permitted merchant fields only)")
public class UpdateSellerProfileRequest {

    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    @Schema(description = "Updated business or trade name", example = "Acme Retail Enterprises Ltd")
    private String businessName;

    @Email(message = "Invalid business email format")
    @Size(max = 255, message = "Business email must not exceed 255 characters")
    @Schema(description = "Updated business contact email", example = "billing@acmeretail.com")
    private String businessEmail;

    @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Business phone must be a valid phone number with 7 to 20 digits")
    @Schema(description = "Updated business contact phone number", example = "+919876543299")
    private String businessPhone;

    @Size(max = 50, message = "Tax identifier must not exceed 50 characters")
    @Schema(description = "Updated tax registration identifier (GSTIN, PAN)", example = "29ABCDE1234F1Z6")
    private String taxIdentifier;
}
