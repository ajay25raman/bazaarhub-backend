package com.bazaarhub.backend.payment.application.dto.response;

import com.bazaarhub.backend.payment.domain.model.TransactionStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO representing an individual gateway transaction audit log.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Individual transaction or attempt log associated with a payment")
public class PaymentTransactionResponse {

    @Schema(description = "Transaction ID", example = "1201")
    private Long id;

    @Schema(description = "External gateway transaction / payment / refund ID", example = "pay_P9yH9y456fghij")
    private String gatewayTransactionId;

    @Schema(description = "Transaction category", example = "CAPTURE")
    private TransactionType transactionType;

    @Schema(description = "Transaction amount in INR", example = "1499.00")
    private BigDecimal amount;

    @Schema(description = "Status of transaction attempt", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Gateway error reason if transaction failed", example = "Payment declined by issuing bank")
    private String failureReason;

    @Schema(description = "Timestamp when transaction was confirmed by gateway")
    private Instant processedAt;

    @Schema(description = "Timestamp when transaction record was created")
    private Instant createdAt;
}
