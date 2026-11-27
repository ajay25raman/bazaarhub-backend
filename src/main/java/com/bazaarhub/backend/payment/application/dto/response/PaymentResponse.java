package com.bazaarhub.backend.payment.application.dto.response;

import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed DTO representation of a Payment entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detailed payment status, order references, and transaction lifecycle")
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "501")
    private Long id;

    @Schema(description = "Internal Payment Reference", example = "PAY-A1B2C3D4E5F6")
    private String paymentReference;

    @Schema(description = "Internal Order ID", example = "1001")
    private Long orderId;

    @Schema(description = "Order Number", example = "ORD-20261114-001")
    private String orderNumber;

    @Schema(description = "Payment Gateway identifier", example = "RAZORPAY")
    private String gateway;

    @Schema(description = "Razorpay Order ID", example = "order_O8xG8x123abcde")
    private String gatewayOrderId;

    @Schema(description = "Razorpay Payment ID", example = "pay_P9yH9y456fghij")
    private String gatewayPaymentId;

    @Schema(description = "Total payment amount", example = "1499.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Customer payment method used", example = "upi")
    private String paymentMethod;

    @Schema(description = "Payment lifecycle status", example = "CAPTURED")
    private PaymentStatus status;

    @Schema(description = "Timestamp when payment was successfully captured")
    private Instant paidAt;

    @Schema(description = "Reason for failure if payment was unsuccessful")
    private String failureReason;

    @Schema(description = "Timestamp when payment record was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when payment record was last modified")
    private Instant updatedAt;

    @Schema(description = "List of associated transaction attempts")
    @Builder.Default
    private List<PaymentTransactionResponse> transactions = new ArrayList<>();
}
