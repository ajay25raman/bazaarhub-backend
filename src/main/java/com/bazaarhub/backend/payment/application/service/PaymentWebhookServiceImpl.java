package com.bazaarhub.backend.payment.application.service;

import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.application.dto.response.WebhookProcessingResult;
import com.bazaarhub.backend.payment.domain.exception.PaymentVerificationException;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.domain.model.RefundStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionType;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.RefundEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentTransactionRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataRefundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookServiceImpl.class);

    private final PaymentGateway paymentGateway;
    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataPaymentTransactionRepository transactionRepository;
    private final SpringDataRefundRepository refundRepository;
    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final OrderStatusTransitionService orderStatusTransitionService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookServiceImpl(PaymentGateway paymentGateway,
                                     SpringDataPaymentRepository paymentRepository,
                                     SpringDataPaymentTransactionRepository transactionRepository,
                                     SpringDataRefundRepository refundRepository,
                                     SpringDataOrderRepository orderRepository,
                                     SpringDataSellerOrderRepository sellerOrderRepository,
                                     OrderStatusTransitionService orderStatusTransitionService,
                                     ObjectMapper objectMapper) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderStatusTransitionService = orderStatusTransitionService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public WebhookProcessingResult processWebhook(String rawPayload, String signatureHeader) {
        log.info("[RAZORPAY WEBHOOK RECEIVED] Validating payload signature");

        boolean isValid = paymentGateway.verifyWebhookSignature(rawPayload, signatureHeader);
        if (!isValid) {
            log.error("[RAZORPAY WEBHOOK ERROR] Signature mismatch against raw payload");
            throw new PaymentVerificationException("Invalid Razorpay webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String event = root.path("event").asText();
            log.info("[RAZORPAY WEBHOOK EVENT] Processing event: {}", event);

            return switch (event) {
                case "payment.captured" -> handlePaymentCaptured(root, rawPayload, event);
                case "payment.failed" -> handlePaymentFailed(root, rawPayload, event);
                case "refund.processed" -> handleRefundProcessed(root, rawPayload, event);
                default -> {
                    log.info("[RAZORPAY WEBHOOK UNHANDLED] Event {} acknowledged with no-op", event);
                    yield WebhookProcessingResult.builder()
                            .success(true)
                            .event(event)
                            .message("Event ignored (no matching handler)")
                            .build();
                }
            };
        } catch (PaymentVerificationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[RAZORPAY WEBHOOK EXCEPTION] Error parsing webhook payload: {}", ex.getMessage(), ex);
            return WebhookProcessingResult.builder()
                    .success(false)
                    .event("UNKNOWN")
                    .message("Failed to process webhook payload: " + ex.getMessage())
                    .build();
        }
    }

    private WebhookProcessingResult handlePaymentCaptured(JsonNode root, String rawPayload, String event) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String gatewayPaymentId = entity.path("id").asText();
        String gatewayOrderId = entity.path("order_id").asText();
        String method = entity.path("method").asText();

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByGatewayOrderId(gatewayOrderId);
        if (paymentOpt.isEmpty() && gatewayPaymentId != null && !gatewayPaymentId.isBlank()) {
            paymentOpt = paymentRepository.findByGatewayPaymentId(gatewayPaymentId);
        }

        if (paymentOpt.isEmpty()) {
            log.warn("[RAZORPAY WEBHOOK] Payment record not found for order_id: {}, payment_id: {}",
                    gatewayOrderId, gatewayPaymentId);
            return WebhookProcessingResult.builder()
                    .success(false)
                    .event(event)
                    .message("Payment record not found for order_id: " + gatewayOrderId)
                    .build();
        }

        PaymentEntity payment = paymentOpt.get();

        // Idempotency: If already captured, don't duplicate state transitions
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            log.info("[RAZORPAY WEBHOOK IDEMPOTENT] Payment {} already CAPTURED", payment.getPaymentReference());
            return WebhookProcessingResult.builder()
                    .success(true)
                    .event(event)
                    .message("Payment already captured (idempotent)")
                    .paymentReference(payment.getPaymentReference())
                    .build();
        }

        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaidAt(Instant.now());
        if (method != null && !method.isBlank()) {
            payment.setPaymentMethod(method);
        }

        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .payment(payment)
                .gatewayTransactionId(gatewayPaymentId)
                .transactionType(TransactionType.CAPTURE)
                .amount(payment.getAmount())
                .status(TransactionStatus.SUCCESS)
                .gatewayResponse(rawPayload)
                .processedAt(Instant.now())
                .build();
        payment.addTransaction(tx);

        OrderEntity order = payment.getOrder();
        order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID);
        order.setUpdatedAt(Instant.now());

        if (order.getStatus() == OrderStatus.PENDING) {
            order = orderStatusTransitionService.transitionOrderStatus(
                    order, OrderStatus.CONFIRMED, "Payment captured via Razorpay webhook (" + gatewayPaymentId + ")", order.getUser());

            List<SellerOrderEntity> sellerOrders = sellerOrderRepository.findByOrderId(order.getId());
            for (SellerOrderEntity sellerOrder : sellerOrders) {
                if (sellerOrder.getStatus() == SellerOrderStatus.PENDING) {
                    orderStatusTransitionService.transitionSellerOrderStatus(
                            sellerOrder, SellerOrderStatus.CONFIRMED, "Payment captured via webhook", order.getUser());
                }
            }
        } else {
            orderRepository.save(order);
        }

        paymentRepository.save(payment);
        log.info("[RAZORPAY WEBHOOK SUCCESS] Payment {} captured for order {}",
                payment.getPaymentReference(), order.getOrderNumber());

        return WebhookProcessingResult.builder()
                .success(true)
                .event(event)
                .message("Payment captured and order confirmed successfully")
                .paymentReference(payment.getPaymentReference())
                .build();
    }

    private WebhookProcessingResult handlePaymentFailed(JsonNode root, String rawPayload, String event) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String gatewayPaymentId = entity.path("id").asText();
        String gatewayOrderId = entity.path("order_id").asText();
        String errorDesc = entity.path("error_description").asText();

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByGatewayOrderId(gatewayOrderId);
        if (paymentOpt.isEmpty() && gatewayPaymentId != null && !gatewayPaymentId.isBlank()) {
            paymentOpt = paymentRepository.findByGatewayPaymentId(gatewayPaymentId);
        }

        if (paymentOpt.isPresent()) {
            PaymentEntity payment = paymentOpt.get();
            if (payment.getStatus() != PaymentStatus.CAPTURED) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(errorDesc);

                PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                        .payment(payment)
                        .gatewayTransactionId(gatewayPaymentId)
                        .transactionType(TransactionType.PAYMENT)
                        .amount(payment.getAmount())
                        .status(TransactionStatus.FAILED)
                        .failureReason(errorDesc)
                        .gatewayResponse(rawPayload)
                        .processedAt(Instant.now())
                        .build();
                payment.addTransaction(tx);
                paymentRepository.save(payment);

                OrderEntity order = payment.getOrder();
                order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.FAILED);
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);

                log.info("[RAZORPAY WEBHOOK FAILED] Payment {} marked FAILED: {}",
                        payment.getPaymentReference(), errorDesc);

                return WebhookProcessingResult.builder()
                        .success(true)
                        .event(event)
                        .message("Payment failure recorded: " + errorDesc)
                        .paymentReference(payment.getPaymentReference())
                        .build();
            }
        }

        return WebhookProcessingResult.builder()
                .success(true)
                .event(event)
                .message("Payment failure event processed")
                .build();
    }

    private WebhookProcessingResult handleRefundProcessed(JsonNode root, String rawPayload, String event) {
        JsonNode entity = root.path("payload").path("refund").path("entity");
        String gatewayRefundId = entity.path("id").asText();

        Optional<RefundEntity> refundOpt = refundRepository.findByGatewayRefundId(gatewayRefundId);
        if (refundOpt.isPresent()) {
            RefundEntity refund = refundOpt.get();
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setProcessedAt(Instant.now());
            refundRepository.save(refund);

            log.info("[RAZORPAY WEBHOOK REFUND] Refund {} completed", refund.getRefundReference());
            return WebhookProcessingResult.builder()
                    .success(true)
                    .event(event)
                    .message("Refund processed successfully")
                    .paymentReference(refund.getPayment().getPaymentReference())
                    .build();
        }

        return WebhookProcessingResult.builder()
                .success(true)
                .event(event)
                .message("Refund event processed")
                .build();
    }
}
