package com.bazaarhub.backend.payment.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for opting Cash on Delivery (COD) as the payment method for an order.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to select Cash on Delivery (COD) for an order")
public class SelectCodPaymentRequest {

    @NotNull(message = "Order ID is mandatory")
    @Schema(description = "ID of the order to configure for COD", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;
}
