package com.bazaarhub.backend.delivery.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Address and contact details for pickup or delivery")
public class DeliveryAddressInfo {

    @Schema(description = "Contact or business name", example = "Sharma Electronics / Rahul Verma")
    private String contactName;

    @Schema(description = "Contact phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Primary street address", example = "Shop 4, MG Road")
    private String addressLine1;

    @Schema(description = "Secondary address / suite", example = "Near City Tower")
    private String addressLine2;

    @Schema(description = "Landmark", example = "Opposite Metro Station")
    private String landmark;

    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Schema(description = "Postal code", example = "400001")
    private String postalCode;

    @Schema(description = "Country", example = "India")
    private String country;

    @Schema(description = "GPS Latitude", example = "19.076090")
    private BigDecimal latitude;

    @Schema(description = "GPS Longitude", example = "72.877426")
    private BigDecimal longitude;
}
