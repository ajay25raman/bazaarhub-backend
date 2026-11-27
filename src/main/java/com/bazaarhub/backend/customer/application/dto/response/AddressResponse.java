package com.bazaarhub.backend.customer.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer delivery address response")
public class AddressResponse {

    @Schema(description = "Address ID", example = "101")
    private Long id;

    @Schema(description = "Owner User ID", example = "42")
    private Long userId;

    @Schema(description = "Address type category", example = "HOME")
    private String addressType;

    @Schema(description = "Recipient name", example = "Alice Johnson")
    private String recipientName;

    @Schema(description = "Recipient phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Address line 1", example = "Flat 402, Lotus Heights")
    private String addressLine1;

    @Schema(description = "Address line 2", example = "Opposite Metro Station")
    private String addressLine2;

    @Schema(description = "Prominent landmark", example = "Near City Mall")
    private String landmark;

    @Schema(description = "City", example = "Bengaluru")
    private String city;

    @Schema(description = "State / Province", example = "Karnataka")
    private String state;

    @Schema(description = "Postal / PIN code", example = "560001")
    private String postalCode;

    @Schema(description = "Country", example = "India")
    private String country;

    @Schema(description = "GPS Latitude coordinate", example = "12.9715987")
    private BigDecimal latitude;

    @Schema(description = "GPS Longitude coordinate", example = "77.5945627")
    private BigDecimal longitude;

    @com.fasterxml.jackson.annotation.JsonProperty("isDefault")
    @Schema(description = "Whether this address is customer's primary default address", example = "true")
    private boolean isDefault;

    @Schema(description = "Creation timestamp", example = "2026-03-01T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-01T12:30:00Z")
    private Instant updatedAt;
}
