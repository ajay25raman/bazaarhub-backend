package com.bazaarhub.backend.payment.application.dto.response;

import com.bazaarhub.backend.payment.domain.model.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Detailed DTO representation of a Refund entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Refund audit details and current status")
public class RefundResponse {

    @Schema(description = "Refund ID", example = "301")
    private Long id;

    @Schema(description = "Internal Refund Reference", example = "RFND-F1E2D3C4B5A6")
    private String refundReference;

    @Schema(description = "Associated Order ID", example = "1001")
    private Long orderId;

    @Schema(description = "Associated Order Number", example = "ORD-20261114-001")
    private String orderNumber;

    @Schema(description = "Associated Payment ID", example = "501")
    private Long paymentId;

    @Schema(description = "Associated Payment Reference", example = "PAY-A1B2C3D4E5F6")
    private String paymentReference;

    @Schema(description = "Razorpay Gateway Refund ID", example = "rfnd_R1sT1u123vwxyz")
    private String gatewayRefundId;

    @Schema(description = "Refunded amount in INR", example = "499.00")
    private BigDecimal refundAmount;

    @Schema(description = "Reason provided for refund", example = "Damaged item received in transit")
    private String reason;

    @Schema(description = "Refund lifecycle status", example = "COMPLETED")
    private RefundStatus status;

    @Schema(description = "User ID who requested refund", example = "1")
    private Long requestedById;

    @Schema(description = "User ID who processed/authorized refund", example = "1")
    private Long processedById;

    @Schema(description = "Timestamp when refund was processed")
    private Instant processedAt;

    @Schema(description = "Rejection justification if refund was rejected")
    private String rejectionReason;

    @Schema(description = "Timestamp when refund record was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when refund record was updated")
    private Instant updatedAt;
}
