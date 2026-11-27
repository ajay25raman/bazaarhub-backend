package com.bazaarhub.backend.payment;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.exception.OrderNotFoundException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.application.dto.request.CollectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.CreateRazorpayOrderRequest;
import com.bazaarhub.backend.payment.application.dto.request.SelectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.VerifyRazorpayPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.response.PaymentResponse;
import com.bazaarhub.backend.payment.application.dto.response.RazorpayOrderCheckoutResponse;
import com.bazaarhub.backend.payment.application.mapper.PaymentMapper;
import com.bazaarhub.backend.payment.application.service.PaymentServiceImpl;
import com.bazaarhub.backend.payment.domain.exception.InvalidPaymentOperationException;
import com.bazaarhub.backend.payment.domain.exception.PaymentNotFoundException;
import com.bazaarhub.backend.payment.domain.exception.PaymentVerificationException;
import com.bazaarhub.backend.payment.domain.exception.UnauthorizedPaymentAccessException;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.infrastructure.config.CodProperties;
import com.bazaarhub.backend.payment.infrastructure.config.RazorpayProperties;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayOrderResponse;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayPaymentDetails;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private SpringDataPaymentRepository paymentRepository;

    @Mock
    private SpringDataPaymentTransactionRepository transactionRepository;

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private OrderStatusTransitionService orderStatusTransitionService;

    @Mock
    private PaymentGateway paymentGateway;

    @Spy
    private PaymentMapper paymentMapper = new PaymentMapper();

    @Mock
    private RazorpayProperties razorpayProperties;

    @Mock
    private CodProperties codProperties;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UserEntity customer;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        customer = UserEntity.builder()
                .id(10L)
                .fullName("Rajesh Kumar")
                .email("rajesh@example.com")
                .phone("+919876543210")
                .build();

        order = OrderEntity.builder()
                .id(1001L)
                .orderNumber("ORD-20261114-1001")
                .user(customer)
                .grandTotal(new BigDecimal("1499.00"))
                .status(OrderStatus.PENDING)
                .paymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Create Razorpay Order creates pending payment and returns checkout parameters")
    void testCreateRazorpayOrder_Success() {
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(1001L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_testkey123");

        RazorpayOrderResponse gatewayResponse = RazorpayOrderResponse.builder()
                .id("order_Gwy12345")
                .amount(149900L)
                .amountDecimal(new BigDecimal("1499.00"))
                .currency("INR")
                .status("created")
                .build();

        when(paymentGateway.createRazorpayOrder(eq(order.getOrderNumber()), eq(new BigDecimal("1499.00")), eq("INR"), anyMap()))
                .thenReturn(gatewayResponse);

        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
            PaymentEntity p = invocation.getArgument(0);
            p.setId(501L);
            return p;
        });

        RazorpayOrderCheckoutResponse response = paymentService.createRazorpayOrder(10L, new CreateRazorpayOrderRequest(1001L));

        assertNotNull(response);
        assertEquals(1001L, response.getOrderId());
        assertEquals("ORD-20261114-1001", response.getOrderNumber());
        assertEquals("order_Gwy12345", response.getRazorpayOrderId());
        assertEquals(149900L, response.getAmountInPaise());
        assertEquals("rzp_test_testkey123", response.getKeyId());
        assertEquals("Rajesh Kumar", response.getCustomerName());

        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("Create Razorpay Order reuses existing pending Razorpay order (idempotency)")
    void testCreateRazorpayOrder_ReusesExistingPending() {
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_testkey123");

        PaymentEntity existing = PaymentEntity.builder()
                .id(501L)
                .order(order)
                .paymentReference("PAY-EXISTING123")
                .gateway("RAZORPAY")
                .gatewayOrderId("order_EXISTING_999")
                .amount(new BigDecimal("1499.00"))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(1001L, PaymentStatus.PENDING))
                .thenReturn(Optional.of(existing));

        RazorpayOrderCheckoutResponse response = paymentService.createRazorpayOrder(10L, new CreateRazorpayOrderRequest(1001L));

        assertNotNull(response);
        assertEquals("order_EXISTING_999", response.getRazorpayOrderId());
        assertEquals("PAY-EXISTING123", response.getPaymentReference());
        verify(paymentGateway, never()).createRazorpayOrder(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Create Razorpay Order throws UnauthorizedPaymentAccessException when customer does not own order")
    void testCreateRazorpayOrder_UnauthorizedUser() {
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedPaymentAccessException.class, () ->
                paymentService.createRazorpayOrder(99L, new CreateRazorpayOrderRequest(1001L)));
    }

    @Test
    @DisplayName("Create Razorpay Order throws InvalidPaymentOperationException when order is cancelled or paid")
    void testCreateRazorpayOrder_InvalidOrderState() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        assertThrows(InvalidPaymentOperationException.class, () ->
                paymentService.createRazorpayOrder(10L, new CreateRazorpayOrderRequest(1001L)));

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID);
        assertThrows(InvalidPaymentOperationException.class, () ->
                paymentService.createRazorpayOrder(10L, new CreateRazorpayOrderRequest(1001L)));
    }

    @Test
    @DisplayName("Verify Razorpay Payment succeeds and updates order and payment to CAPTURED")
    void testVerifyRazorpayPayment_Success() {
        PaymentEntity payment = PaymentEntity.builder()
                .id(501L)
                .paymentReference("PAY-REF123")
                .order(order)
                .gateway("RAZORPAY")
                .gatewayOrderId("order_Gwy12345")
                .amount(new BigDecimal("1499.00"))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentGateway.verifyPaymentSignature("order_Gwy12345", "pay_P9yH9y456fghij", "valid_sig"))
                .thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_Gwy12345")).thenReturn(Optional.of(payment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(paymentGateway.fetchPaymentDetails("pay_P9yH9y456fghij")).thenReturn(
                RazorpayPaymentDetails.builder().id("pay_P9yH9y456fghij").method("upi").status("captured").build());

        when(orderStatusTransitionService.transitionOrderStatus(eq(order), eq(OrderStatus.CONFIRMED), anyString(), any()))
                .thenReturn(order);
        when(sellerOrderRepository.findByOrderId(1001L)).thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArgument(0));

        VerifyRazorpayPaymentRequest request = VerifyRazorpayPaymentRequest.builder()
                .orderId(1001L)
                .razorpayOrderId("order_Gwy12345")
                .razorpayPaymentId("pay_P9yH9y456fghij")
                .razorpaySignature("valid_sig")
                .build();

        PaymentResponse response = paymentService.verifyRazorpayPayment(10L, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.CAPTURED, response.getStatus());
        assertEquals("pay_P9yH9y456fghij", response.getGatewayPaymentId());
        assertEquals("upi", response.getPaymentMethod());
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    @DisplayName("Verify Razorpay Payment throws PaymentVerificationException on signature mismatch")
    void testVerifyRazorpayPayment_InvalidSignature() {
        when(paymentGateway.verifyPaymentSignature("order_123", "pay_456", "bad_sig")).thenReturn(false);

        VerifyRazorpayPaymentRequest request = VerifyRazorpayPaymentRequest.builder()
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_456")
                .razorpaySignature("bad_sig")
                .build();

        assertThrows(PaymentVerificationException.class, () ->
                paymentService.verifyRazorpayPayment(10L, request));
    }

    @Test
    @DisplayName("Select COD Payment configures payment and confirms order")
    void testSelectCodPayment_Success() {
        when(codProperties.isEnabled()).thenReturn(true);
        when(codProperties.getMaxAmount()).thenReturn(new BigDecimal("50000.00"));
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(1001L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(orderStatusTransitionService.transitionOrderStatus(eq(order), eq(OrderStatus.CONFIRMED), anyString(), any()))
                .thenReturn(order);
        when(sellerOrderRepository.findByOrderId(1001L)).thenReturn(Collections.emptyList());

        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> {
            PaymentEntity p = i.getArgument(0);
            p.setId(502L);
            return p;
        });

        PaymentResponse response = paymentService.selectCodPayment(10L, new SelectCodPaymentRequest(1001L));

        assertNotNull(response);
        assertEquals("COD", response.getGateway());
        assertEquals("CASH", response.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Select COD Payment throws InvalidPaymentOperationException when order exceeds max limit")
    void testSelectCodPayment_LimitExceeded() {
        when(codProperties.isEnabled()).thenReturn(true);
        when(codProperties.getMaxAmount()).thenReturn(new BigDecimal("1000.00")); // Order is 1499.00
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        assertThrows(InvalidPaymentOperationException.class, () ->
                paymentService.selectCodPayment(10L, new SelectCodPaymentRequest(1001L)));
    }

    @Test
    @DisplayName("Collect COD Payment marks payment as CAPTURED and order as PAID")
    void testCollectCodPayment_Success() {
        PaymentEntity codPayment = PaymentEntity.builder()
                .id(502L)
                .paymentReference("COD-123456")
                .order(order)
                .gateway("COD")
                .paymentMethod("CASH")
                .amount(new BigDecimal("1499.00"))
                .status(PaymentStatus.PENDING)
                .build();

        UserEntity admin = UserEntity.builder().id(1L).fullName("Admin User").build();

        when(paymentRepository.findById(502L)).thenReturn(Optional.of(codPayment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.collectCodPayment(1L, 502L, new CollectCodPaymentRequest(new BigDecimal("1499.00"), "Cash delivered by carrier"));

        assertNotNull(response);
        assertEquals(PaymentStatus.CAPTURED, response.getStatus());
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID, order.getPaymentStatus());
        verify(orderRepository, times(1)).save(order);
    }
}
