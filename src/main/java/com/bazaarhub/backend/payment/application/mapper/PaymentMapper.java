package com.bazaarhub.backend.payment.application.mapper;

import com.bazaarhub.backend.payment.application.dto.response.PaymentResponse;
import com.bazaarhub.backend.payment.application.dto.response.PaymentSummaryResponse;
import com.bazaarhub.backend.payment.application.dto.response.PaymentTransactionResponse;
import com.bazaarhub.backend.payment.application.dto.response.RefundResponse;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.RefundEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        List<PaymentTransactionResponse> transactions = entity.getTransactions() != null
                ? entity.getTransactions().stream().map(this::toTransactionResponse).toList()
                : Collections.emptyList();

        return PaymentResponse.builder()
                .id(entity.getId())
                .paymentReference(entity.getPaymentReference())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .orderNumber(entity.getOrder() != null ? entity.getOrder().getOrderNumber() : null)
                .gateway(entity.getGateway())
                .gatewayOrderId(entity.getGatewayOrderId())
                .gatewayPaymentId(entity.getGatewayPaymentId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .paidAt(entity.getPaidAt())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .transactions(transactions)
                .build();
    }

    public PaymentSummaryResponse toSummary(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentSummaryResponse.builder()
                .id(entity.getId())
                .paymentReference(entity.getPaymentReference())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .orderNumber(entity.getOrder() != null ? entity.getOrder().getOrderNumber() : null)
                .gateway(entity.getGateway())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .paidAt(entity.getPaidAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PaymentTransactionResponse toTransactionResponse(PaymentTransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentTransactionResponse.builder()
                .id(entity.getId())
                .gatewayTransactionId(entity.getGatewayTransactionId())
                .transactionType(entity.getTransactionType())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .processedAt(entity.getProcessedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RefundResponse toRefundResponse(RefundEntity entity) {
        if (entity == null) {
            return null;
        }

        return RefundResponse.builder()
                .id(entity.getId())
                .refundReference(entity.getRefundReference())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .orderNumber(entity.getOrder() != null ? entity.getOrder().getOrderNumber() : null)
                .paymentId(entity.getPayment() != null ? entity.getPayment().getId() : null)
                .paymentReference(entity.getPayment() != null ? entity.getPayment().getPaymentReference() : null)
                .gatewayRefundId(entity.getGatewayRefundId())
                .refundAmount(entity.getRefundAmount())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .requestedById(entity.getRequestedBy() != null ? entity.getRequestedBy().getId() : null)
                .processedById(entity.getProcessedBy() != null ? entity.getProcessedBy().getId() : null)
                .processedAt(entity.getProcessedAt())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
