package com.bazaarhub.backend.payment.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for validating Razorpay checkout signature upon payment completion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment verification payload returned by Razorpay Checkout SDK")
public class VerifyRazorpayPaymentRequest {

    @Schema(description = "Optional order ID to corroborate with payment record", example = "1001")
    private Long orderId;

    @NotBlank(message = "Razorpay order ID is mandatory")
    @Schema(description = "Razorpay order ID returned during checkout", example = "order_O8xG8x123abcde", requiredMode = Schema.RequiredMode.REQUIRED)
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is mandatory")
    @Schema(description = "Razorpay payment ID generated for the transaction", example = "pay_P9yH9y456fghij", requiredMode = Schema.RequiredMode.REQUIRED)
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is mandatory")
    @Schema(description = "HMAC SHA256 signature returned by Razorpay Checkout", example = "9b6422...53df8a", requiredMode = Schema.RequiredMode.REQUIRED)
    private String razorpaySignature;
}
