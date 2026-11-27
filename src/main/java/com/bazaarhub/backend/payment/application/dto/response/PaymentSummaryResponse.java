package com.bazaarhub.backend.payment.application.dto.response;

import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight DTO representation of Payment for paginated listings.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Summary payment representation for list views")
public class PaymentSummaryResponse {

    @Schema(description = "Payment ID", example = "501")
    private Long id;

    @Schema(description = "Internal Payment Reference", example = "PAY-A1B2C3D4E5F6")
    private String paymentReference;

    @Schema(description = "Associated Order ID", example = "1001")
    private Long orderId;

    @Schema(description = "Associated Order Number", example = "ORD-20261114-001")
    private String orderNumber;

    @Schema(description = "Payment Gateway", example = "RAZORPAY")
    private String gateway;

    @Schema(description = "Payment Amount", example = "1499.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Payment Method", example = "upi")
    private String paymentMethod;

    @Schema(description = "Current payment status", example = "CAPTURED")
    private PaymentStatus status;

    @Schema(description = "Timestamp when payment was marked paid")
    private Instant paidAt;

    @Schema(description = "Payment creation timestamp")
    private Instant createdAt;
}
