package com.bazaarhub.backend.payment.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for recording COD cash collection by administrators or fulfillment personnel.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to mark COD payment as collected")
public class CollectCodPaymentRequest {

    @DecimalMin(value = "0.01", message = "Collected amount must be greater than zero")
    @Schema(description = "Amount collected in cash; defaults to order grand total if null", example = "1499.00")
    private BigDecimal amount;

    @Schema(description = "Optional transaction receipt or collector reference note", example = "Collected by Hub Manager A.K.")
    private String transactionReference;
}
