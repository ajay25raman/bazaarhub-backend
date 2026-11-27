package com.bazaarhub.backend.shipping.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to estimate internal shipping rate based on distance, weight, and order value")
public class ShippingRateQuoteRequest {

    @Positive(message = "Seller order ID must be positive")
    @Schema(description = "Seller order ID if calculating for an existing order", example = "101")
    private Long sellerOrderId;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Origin postal code must be a valid 6-digit Indian PIN code")
    @Schema(description = "Origin store pincode", example = "560001")
    private String originPostalCode;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Destination postal code must be a valid 6-digit Indian PIN code")
    @Schema(description = "Destination delivery pincode", example = "560038")
    private String destinationPostalCode;

    @Size(max = 100, message = "Origin city must not exceed 100 characters")
    @Schema(description = "Origin store city", example = "Bengaluru")
    private String originCity;

    @Size(max = 100, message = "Destination city must not exceed 100 characters")
    @Schema(description = "Destination city", example = "Bengaluru")
    private String destinationCity;

    @Positive(message = "Total parcel weight must be positive")
    @Schema(description = "Total parcel weight in kilograms", example = "1.500")
    private BigDecimal weightKg;

    @PositiveOrZero(message = "Subtotal must be zero or positive")
    @Schema(description = "Cart / sub-order item subtotal for free shipping evaluation", example = "750.00")
    private BigDecimal subtotal;
}
