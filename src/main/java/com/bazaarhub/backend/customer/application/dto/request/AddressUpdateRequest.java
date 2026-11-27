package com.bazaarhub.backend.customer.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer delivery address update request")
public class AddressUpdateRequest {

    @Schema(description = "Address category type (e.g. HOME, WORK, OTHER)", example = "HOME")
    @Size(max = 30, message = "Address type must not exceed 30 characters")
    private String addressType;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150, message = "Recipient name must not exceed 150 characters")
    @Schema(description = "Name of package recipient", example = "Alice Johnson")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    @Schema(description = "Contact phone number for delivery", example = "+919876543210")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Schema(description = "Building, apartment, street address", example = "Flat 402, Lotus Heights")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    @Schema(description = "Suite, area, or locality", example = "Opposite Metro Station")
    private String addressLine2;

    @Size(max = 255, message = "Landmark must not exceed 255 characters")
    @Schema(description = "Prominent nearby landmark", example = "Near City Mall")
    private String landmark;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City or town", example = "Bengaluru")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State, province or region", example = "Karnataka")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Pattern(regexp = "^[0-9A-Za-z\\s-]{4,12}$", message = "Postal code format is invalid")
    @Schema(description = "PIN or postal code", example = "560001")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Schema(description = "Country", example = "India")
    private String country;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Latitude coordinate", example = "12.9715987")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Longitude coordinate", example = "77.5945627")
    private BigDecimal longitude;

    @com.fasterxml.jackson.annotation.JsonProperty("isDefault")
    @Schema(description = "Flag indicating whether this address should be the primary default", example = "true")
    private Boolean isDefault;
}
