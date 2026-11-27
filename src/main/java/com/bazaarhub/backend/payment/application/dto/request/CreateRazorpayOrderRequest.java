package com.bazaarhub.backend.payment.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for initializing a Razorpay order from an existing purchase order.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to initiate Razorpay checkout order for a placed order")
public class CreateRazorpayOrderRequest {

    @NotNull(message = "Order ID is mandatory")
    @Schema(description = "ID of the customer order to be paid", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;
}
