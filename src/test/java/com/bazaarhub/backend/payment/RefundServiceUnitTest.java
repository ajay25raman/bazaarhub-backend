package com.bazaarhub.backend.payment;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.payment.application.dto.request.CreateRefundRequest;
import com.bazaarhub.backend.payment.application.dto.response.RefundResponse;
import com.bazaarhub.backend.payment.application.mapper.PaymentMapper;
import com.bazaarhub.backend.payment.application.service.RefundServiceImpl;
import com.bazaarhub.backend.payment.domain.exception.InvalidRefundOperationException;
import com.bazaarhub.backend.payment.domain.exception.UnauthorizedPaymentAccessException;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.domain.model.RefundStatus;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayRefundResponse;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.RefundEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataRefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceUnitTest {

    @Mock
    private SpringDataRefundRepository refundRepository;

    @Mock
    private SpringDataPaymentRepository paymentRepository;

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Spy
    private PaymentMapper paymentMapper = new PaymentMapper();

    @InjectMocks
    private RefundServiceImpl refundService;

    private UserEntity admin;
    private UserEntity customer;
    private OrderEntity order;
    private PaymentEntity capturedPayment;

    @BeforeEach
    void setUp() {
        admin = UserEntity.builder().id(1L).fullName("Admin Manager").build();
        customer = UserEntity.builder().id(10L).fullName("Customer One").build();

        order = OrderEntity.builder()
                .id(1001L)
                .orderNumber("ORD-1001")
                .user(customer)
                .grandTotal(new BigDecimal("1499.00"))
                .paymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID)
                .build();

        capturedPayment = PaymentEntity.builder()
                .id(501L)
                .paymentReference("PAY-CAP-501")
                .gateway("RAZORPAY")
                .gatewayOrderId("order_EKwxwAgIt42hQC")
                .gatewayPaymentId("pay_P9yH9y456fghij")
                .order(order)
                .amount(new BigDecimal("1499.00"))
                .status(PaymentStatus.CAPTURED)
                .build();
    }

    @Test
    @DisplayName("Create full refund marks payment as REFUNDED and order as REFUNDED")
    void testCreateRefund_FullRefund_Success() {
        when(paymentRepository.findById(501L)).thenReturn(Optional.of(capturedPayment));
        when(refundRepository.getTotalCommittedRefundAmount(501L)).thenReturn(BigDecimal.ZERO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        RazorpayRefundResponse gatewayResp = RazorpayRefundResponse.builder()
                .id("rfnd_FP8j7n28xQC123")
                .paymentId("pay_P9yH9y456fghij")
                .amount(149900L)
                .amountDecimal(new BigDecimal("1499.00"))
                .status("processed")
                .build();

        when(paymentGateway.initiateRefund(eq("pay_P9yH9y456fghij"), eq(new BigDecimal("1499.00")), eq("Damaged item"), anyString()))
                .thenReturn(gatewayResp);

        when(refundRepository.save(any(RefundEntity.class))).thenAnswer(i -> {
            RefundEntity r = i.getArgument(0);
            r.setId(301L);
            return r;
        });

        CreateRefundRequest request = new CreateRefundRequest(new BigDecimal("1499.00"), "Damaged item");
        RefundResponse response = refundService.createRefund(1L, 501L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1499.00"), response.getRefundAmount());
        assertEquals("rfnd_FP8j7n28xQC123", response.getGatewayRefundId());
        assertEquals(RefundStatus.COMPLETED, response.getStatus());

        assertEquals(PaymentStatus.REFUNDED, capturedPayment.getStatus());
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.REFUNDED, order.getPaymentStatus());

        verify(refundRepository, times(1)).save(any(RefundEntity.class));
        verify(paymentRepository, times(1)).save(capturedPayment);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Create partial refund marks payment and order as PARTIALLY_REFUNDED")
    void testCreateRefund_PartialRefund_Success() {
        when(paymentRepository.findById(501L)).thenReturn(Optional.of(capturedPayment));
        when(refundRepository.getTotalCommittedRefundAmount(501L)).thenReturn(BigDecimal.ZERO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        RazorpayRefundResponse gatewayResp = RazorpayRefundResponse.builder()
                .id("rfnd_PARTIAL_123")
                .paymentId("pay_P9yH9y456fghij")
                .amount(50000L)
                .amountDecimal(new BigDecimal("500.00"))
                .status("processed")
                .build();

        when(paymentGateway.initiateRefund(eq("pay_P9yH9y456fghij"), eq(new BigDecimal("500.00")), eq("Partial goodwill"), anyString()))
                .thenReturn(gatewayResp);

        when(refundRepository.save(any(RefundEntity.class))).thenAnswer(i -> {
            RefundEntity r = i.getArgument(0);
            r.setId(302L);
            return r;
        });

        CreateRefundRequest request = new CreateRefundRequest(new BigDecimal("500.00"), "Partial goodwill");
        RefundResponse response = refundService.createRefund(1L, 501L, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, capturedPayment.getStatus());
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PARTIALLY_REFUNDED, order.getPaymentStatus());
    }

    @Test
    @DisplayName("Create refund throws InvalidRefundOperationException when requested amount exceeds available balance")
    void testCreateRefund_AmountExceedsBalance() {
        when(paymentRepository.findById(501L)).thenReturn(Optional.of(capturedPayment));
        // Already refunded 1000 of 1499, only 499 remaining
        when(refundRepository.getTotalCommittedRefundAmount(501L)).thenReturn(new BigDecimal("1000.00"));

        CreateRefundRequest request = new CreateRefundRequest(new BigDecimal("600.00"), "Excess refund");

        assertThrows(InvalidRefundOperationException.class, () ->
                refundService.createRefund(1L, 501L, request));
    }

    @Test
    @DisplayName("Create refund throws InvalidRefundOperationException when payment is not CAPTURED")
    void testCreateRefund_PaymentNotCaptured() {
        capturedPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(501L)).thenReturn(Optional.of(capturedPayment));

        CreateRefundRequest request = new CreateRefundRequest(new BigDecimal("500.00"), "Refund attempt");

        assertThrows(InvalidRefundOperationException.class, () ->
                refundService.createRefund(1L, 501L, request));
    }

    @Test
    @DisplayName("Get refund by ID enforces ownership checks for non-admin customers")
    void testGetRefundById_OwnershipCheck() {
        RefundEntity refund = RefundEntity.builder()
                .id(301L)
                .order(order)
                .payment(capturedPayment)
                .refundReference("RFND-301")
                .refundAmount(new BigDecimal("500.00"))
                .build();

        when(refundRepository.findById(301L)).thenReturn(Optional.of(refund));

        // Customer owns order (customer.id = 10L) -> succeeds
        RefundResponse resp = refundService.getRefundById(301L, 10L, false);
        assertNotNull(resp);

        // Different user (user.id = 99L) and not admin -> throws UnauthorizedPaymentAccessException
        assertThrows(UnauthorizedPaymentAccessException.class, () ->
                refundService.getRefundById(301L, 99L, false));
    }
}
