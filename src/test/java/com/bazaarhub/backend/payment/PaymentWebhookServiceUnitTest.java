package com.bazaarhub.backend.payment;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.application.dto.response.WebhookProcessingResult;
import com.bazaarhub.backend.payment.application.service.PaymentWebhookServiceImpl;
import com.bazaarhub.backend.payment.domain.exception.PaymentVerificationException;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.domain.model.RefundStatus;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.RefundEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentTransactionRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataRefundRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceUnitTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private SpringDataPaymentRepository paymentRepository;

    @Mock
    private SpringDataPaymentTransactionRepository transactionRepository;

    @Mock
    private SpringDataRefundRepository refundRepository;

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private OrderStatusTransitionService orderStatusTransitionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentWebhookServiceImpl webhookService;

    private UserEntity user;
    private OrderEntity order;
    private PaymentEntity payment;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(10L).fullName("Test User").build();
        order = OrderEntity.builder()
                .id(1001L)
                .orderNumber("ORD-1001")
                .user(user)
                .grandTotal(new BigDecimal("1499.00"))
                .status(OrderStatus.PENDING)
                .paymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PENDING)
                .build();

        payment = PaymentEntity.builder()
                .id(501L)
                .paymentReference("PAY-REF501")
                .gateway("RAZORPAY")
                .gatewayOrderId("order_EKwxwAgIt42hQC")
                .order(order)
                .amount(new BigDecimal("1499.00"))
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Process payment.captured webhook marks payment CAPTURED and confirms order")
    void testProcessWebhook_PaymentCaptured_Success() {
        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_P9yH9y456fghij",
                        "order_id": "order_EKwxwAgIt42hQC",
                        "amount": 149900,
                        "currency": "INR",
                        "status": "captured",
                        "method": "card"
                      }
                    }
                  }
                }
                """;

        when(paymentGateway.verifyWebhookSignature(payload, "valid_sig")).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_EKwxwAgIt42hQC")).thenReturn(Optional.of(payment));
        when(orderStatusTransitionService.transitionOrderStatus(eq(order), eq(OrderStatus.CONFIRMED), anyString(), any()))
                .thenReturn(order);
        when(sellerOrderRepository.findByOrderId(1001L)).thenReturn(Collections.emptyList());

        WebhookProcessingResult result = webhookService.processWebhook(payload, "valid_sig");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("payment.captured", result.getEvent());
        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        assertEquals("pay_P9yH9y456fghij", payment.getGatewayPaymentId());
        assertEquals("card", payment.getPaymentMethod());
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID, order.getPaymentStatus());

        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("Process payment.captured webhook is idempotent when payment is already CAPTURED")
    void testProcessWebhook_PaymentCaptured_Idempotent() {
        payment.setStatus(PaymentStatus.CAPTURED);

        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_P9yH9y456fghij",
                        "order_id": "order_EKwxwAgIt42hQC"
                      }
                    }
                  }
                }
                """;

        when(paymentGateway.verifyWebhookSignature(payload, "valid_sig")).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_EKwxwAgIt42hQC")).thenReturn(Optional.of(payment));

        WebhookProcessingResult result = webhookService.processWebhook(payload, "valid_sig");

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("already captured"));
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    @DisplayName("Process payment.failed webhook marks payment and order as FAILED")
    void testProcessWebhook_PaymentFailed_Success() {
        String payload = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_P9yH9y456fghij",
                        "order_id": "order_EKwxwAgIt42hQC",
                        "error_description": "Card expired"
                      }
                    }
                  }
                }
                """;

        when(paymentGateway.verifyWebhookSignature(payload, "valid_sig")).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_EKwxwAgIt42hQC")).thenReturn(Optional.of(payment));

        WebhookProcessingResult result = webhookService.processWebhook(payload, "valid_sig");

        assertTrue(result.isSuccess());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("Card expired", payment.getFailureReason());
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.FAILED, order.getPaymentStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Process refund.processed webhook updates refund status to COMPLETED")
    void testProcessWebhook_RefundProcessed_Success() {
        RefundEntity refund = RefundEntity.builder()
                .id(301L)
                .refundReference("RFND-12345")
                .gatewayRefundId("rfnd_FP8j7n28xQC123")
                .payment(payment)
                .status(RefundStatus.REQUESTED)
                .build();

        String payload = """
                {
                  "event": "refund.processed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_FP8j7n28xQC123"
                      }
                    }
                  }
                }
                """;

        when(paymentGateway.verifyWebhookSignature(payload, "valid_sig")).thenReturn(true);
        when(refundRepository.findByGatewayRefundId("rfnd_FP8j7n28xQC123")).thenReturn(Optional.of(refund));

        WebhookProcessingResult result = webhookService.processWebhook(payload, "valid_sig");

        assertTrue(result.isSuccess());
        assertEquals(RefundStatus.COMPLETED, refund.getStatus());
        verify(refundRepository, times(1)).save(refund);
    }

    @Test
    @DisplayName("Process webhook throws PaymentVerificationException on invalid signature")
    void testProcessWebhook_InvalidSignature() {
        String payload = "{\"event\":\"payment.captured\"}";
        when(paymentGateway.verifyWebhookSignature(payload, "bad_sig")).thenReturn(false);

        assertThrows(PaymentVerificationException.class, () ->
                webhookService.processWebhook(payload, "bad_sig"));
    }
}
