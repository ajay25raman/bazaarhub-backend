package com.bazaarhub.backend.payment.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Response payload containing configuration and credentials for frontend Razorpay Standard Checkout SDK.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Checkout initialization payload containing Razorpay order parameters")
public class RazorpayOrderCheckoutResponse {

    @Schema(description = "Internal Order ID", example = "1001")
    private Long orderId;

    @Schema(description = "Unique human-readable Order Number", example = "ORD-20261114-001")
    private String orderNumber;

    @Schema(description = "Internal Payment ID", example = "501")
    private Long paymentId;

    @Schema(description = "Unique internal Payment Reference", example = "PAY-A1B2C3D4E5F6")
    private String paymentReference;

    @Schema(description = "Razorpay Order ID", example = "order_O8xG8x123abcde")
    private String razorpayOrderId;

    @Schema(description = "Total payable amount in INR (server authoritative)", example = "1499.00")
    private BigDecimal amount;

    @Schema(description = "Payable amount in paise (1 INR = 100 paise)", example = "149900")
    private Long amountInPaise;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Razorpay Public Key ID to initialize Checkout modal", example = "rzp_test_YourKeyHere")
    private String keyId;

    @Schema(description = "Customer Full Name for prefill", example = "John Doe")
    private String customerName;

    @Schema(description = "Customer Email for prefill", example = "john.doe@example.com")
    private String customerEmail;

    @Schema(description = "Customer Phone for prefill", example = "+919876543210")
    private String customerPhone;

    @Schema(description = "Current payment status", example = "PENDING")
    private String status;
}
