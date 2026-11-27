package com.bazaarhub.backend.payment.application.service;

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
import com.bazaarhub.backend.payment.application.dto.response.PaymentSummaryResponse;
import com.bazaarhub.backend.payment.application.dto.response.RazorpayOrderCheckoutResponse;
import com.bazaarhub.backend.payment.application.mapper.PaymentMapper;
import com.bazaarhub.backend.payment.domain.exception.InvalidPaymentOperationException;
import com.bazaarhub.backend.payment.domain.exception.PaymentNotFoundException;
import com.bazaarhub.backend.payment.domain.exception.PaymentVerificationException;
import com.bazaarhub.backend.payment.domain.exception.UnauthorizedPaymentAccessException;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionType;
import com.bazaarhub.backend.payment.infrastructure.config.CodProperties;
import com.bazaarhub.backend.payment.infrastructure.config.RazorpayProperties;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayOrderResponse;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayPaymentDetails;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataPaymentTransactionRepository transactionRepository;
    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataUserRepository userRepository;
    private final OrderStatusTransitionService orderStatusTransitionService;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;
    private final RazorpayProperties razorpayProperties;
    private final CodProperties codProperties;

    public PaymentServiceImpl(SpringDataPaymentRepository paymentRepository,
                              SpringDataPaymentTransactionRepository transactionRepository,
                              SpringDataOrderRepository orderRepository,
                              SpringDataSellerOrderRepository sellerOrderRepository,
                              SpringDataUserRepository userRepository,
                              OrderStatusTransitionService orderStatusTransitionService,
                              PaymentGateway paymentGateway,
                              PaymentMapper paymentMapper,
                              RazorpayProperties razorpayProperties,
                              CodProperties codProperties) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.userRepository = userRepository;
        this.orderStatusTransitionService = orderStatusTransitionService;
        this.paymentGateway = paymentGateway;
        this.paymentMapper = paymentMapper;
        this.razorpayProperties = razorpayProperties;
        this.codProperties = codProperties;
    }

    @Override
    @Transactional
    public RazorpayOrderCheckoutResponse createRazorpayOrder(Long customerId, CreateRazorpayOrderRequest request) {
        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + request.getOrderId()));

        if (!order.getUser().getId().equals(customerId)) {
            throw new UnauthorizedPaymentAccessException("You do not own order: " + order.getOrderNumber());
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidPaymentOperationException("Cannot initiate payment for cancelled order: " + order.getOrderNumber());
        }

        if (order.getPaymentStatus() == com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID) {
            throw new InvalidPaymentOperationException("Order " + order.getOrderNumber() + " is already paid");
        }

        BigDecimal orderAmount = order.getGrandTotal();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentOperationException("Payable amount must be greater than zero for order: " + order.getOrderNumber());
        }

        long amountInPaise = orderAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        UserEntity customer = userRepository.findById(customerId).orElse(order.getUser());

        // Check for existing pending Razorpay order to enforce idempotency
        Optional<PaymentEntity> existingPending = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                order.getId(), PaymentStatus.PENDING);

        if (existingPending.isPresent() && "RAZORPAY".equalsIgnoreCase(existingPending.get().getGateway())
                && existingPending.get().getGatewayOrderId() != null) {
            PaymentEntity existing = existingPending.get();
            log.info("[RAZORPAY ORDER REUSED] Existing pending Razorpay order {} reused for order {}",
                    existing.getGatewayOrderId(), order.getOrderNumber());

            return RazorpayOrderCheckoutResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .paymentId(existing.getId())
                    .paymentReference(existing.getPaymentReference())
                    .razorpayOrderId(existing.getGatewayOrderId())
                    .amount(orderAmount)
                    .amountInPaise(amountInPaise)
                    .currency("INR")
                    .keyId(razorpayProperties.getKeyId())
                    .customerName(customer.getFullName())
                    .customerEmail(customer.getEmail())
                    .customerPhone(customer.getPhone())
                    .status(PaymentStatus.PENDING.name())
                    .build();
        }

        // Call Razorpay API to generate order
        Map<String, String> notes = Map.of(
                "orderId", String.valueOf(order.getId()),
                "orderNumber", order.getOrderNumber(),
                "customerId", String.valueOf(customerId)
        );

        RazorpayOrderResponse gatewayResponse = paymentGateway.createRazorpayOrder(
                order.getOrderNumber(), orderAmount, "INR", notes);

        String paymentRef = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .paymentReference(paymentRef)
                .gateway("RAZORPAY")
                .gatewayOrderId(gatewayResponse.getId())
                .amount(orderAmount)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .createdBy(customer)
                .updatedBy(customer)
                .build();

        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .payment(payment)
                .gatewayTransactionId(gatewayResponse.getId())
                .transactionType(TransactionType.AUTHORIZATION)
                .amount(orderAmount)
                .status(TransactionStatus.PENDING)
                .createdBy(customer)
                .updatedBy(customer)
                .build();

        payment.addTransaction(transaction);
        PaymentEntity savedPayment = paymentRepository.save(payment);

        log.info("[RAZORPAY ORDER CREATED] Payment record {} created with gateway order ID {} for order {}",
                savedPayment.getPaymentReference(), gatewayResponse.getId(), order.getOrderNumber());

        return RazorpayOrderCheckoutResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .paymentId(savedPayment.getId())
                .paymentReference(savedPayment.getPaymentReference())
                .razorpayOrderId(gatewayResponse.getId())
                .amount(orderAmount)
                .amountInPaise(amountInPaise)
                .currency("INR")
                .keyId(razorpayProperties.getKeyId())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhone())
                .status(PaymentStatus.PENDING.name())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse verifyRazorpayPayment(Long customerId, VerifyRazorpayPaymentRequest request) {
        boolean signatureValid = paymentGateway.verifyPaymentSignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!signatureValid) {
            log.warn("[RAZORPAY VERIFY FAILED] Invalid signature for order: {} and payment: {}",
                    request.getRazorpayOrderId(), request.getRazorpayPaymentId());
            throw new PaymentVerificationException("Cryptographic verification failed: Razorpay payment signature mismatch");
        }

        PaymentEntity payment = paymentRepository.findByGatewayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment record not found for Razorpay order ID: " + request.getRazorpayOrderId()));

        if (!payment.getOrder().getUser().getId().equals(customerId)) {
            throw new UnauthorizedPaymentAccessException("You are not authorized to verify payment for this order");
        }

        // Idempotency: If already captured, return immediate success
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            log.info("[RAZORPAY VERIFY IDEMPOTENT] Payment {} is already CAPTURED", payment.getPaymentReference());
            return paymentMapper.toResponse(payment);
        }

        // Fetch payment details from Razorpay to get accurate payment method
        String paymentMethod = null;
        try {
            RazorpayPaymentDetails details = paymentGateway.fetchPaymentDetails(request.getRazorpayPaymentId());
            paymentMethod = details.getMethod();
        } catch (Exception ex) {
            log.warn("[RAZORPAY DETAILS WARN] Could not fetch payment method: {}", ex.getMessage());
        }

        UserEntity customer = userRepository.findById(customerId).orElse(payment.getOrder().getUser());

        // Update payment state
        payment.setGatewayPaymentId(request.getRazorpayPaymentId());
        if (paymentMethod != null) {
            payment.setPaymentMethod(paymentMethod);
        }
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaidAt(Instant.now());
        payment.setUpdatedBy(customer);

        // Record capture transaction
        PaymentTransactionEntity captureTx = PaymentTransactionEntity.builder()
                .payment(payment)
                .gatewayTransactionId(request.getRazorpayPaymentId())
                .transactionType(TransactionType.CAPTURE)
                .amount(payment.getAmount())
                .status(TransactionStatus.SUCCESS)
                .processedAt(Instant.now())
                .createdBy(customer)
                .updatedBy(customer)
                .build();
        payment.addTransaction(captureTx);

        // Update Order payment status and transition order to CONFIRMED
        OrderEntity order = payment.getOrder();
        order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID);
        order.setUpdatedBy(customer);
        order.setUpdatedAt(Instant.now());

        if (order.getStatus() == OrderStatus.PENDING) {
            order = orderStatusTransitionService.transitionOrderStatus(
                    order, OrderStatus.CONFIRMED, "Payment verified successfully via Razorpay (" + request.getRazorpayPaymentId() + ")", customer);

            List<SellerOrderEntity> sellerOrders = sellerOrderRepository.findByOrderId(order.getId());
            for (SellerOrderEntity sellerOrder : sellerOrders) {
                if (sellerOrder.getStatus() == SellerOrderStatus.PENDING) {
                    orderStatusTransitionService.transitionSellerOrderStatus(
                            sellerOrder, SellerOrderStatus.CONFIRMED, "Payment verified via Razorpay", customer);
                }
            }
        } else {
            order = orderRepository.save(order);
        }

        PaymentEntity savedPayment = paymentRepository.save(payment);
        log.info("[RAZORPAY PAYMENT CAPTURED] Payment {} captured for order {}", savedPayment.getPaymentReference(), order.getOrderNumber());

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse selectCodPayment(Long customerId, SelectCodPaymentRequest request) {
        if (!codProperties.isEnabled()) {
            throw new InvalidPaymentOperationException("Cash on Delivery (COD) is currently disabled on this platform");
        }

        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + request.getOrderId()));

        if (!order.getUser().getId().equals(customerId)) {
            throw new UnauthorizedPaymentAccessException("You do not own order: " + order.getOrderNumber());
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidPaymentOperationException("Cannot select COD for cancelled order: " + order.getOrderNumber());
        }

        if (order.getPaymentStatus() == com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID) {
            throw new InvalidPaymentOperationException("Order " + order.getOrderNumber() + " is already paid");
        }

        if (codProperties.getMaxAmount() != null && order.getGrandTotal().compareTo(codProperties.getMaxAmount()) > 0) {
            throw new InvalidPaymentOperationException(String.format(
                    "Order total ₹%s exceeds the maximum permissible COD limit of ₹%s",
                    order.getGrandTotal(), codProperties.getMaxAmount()));
        }

        UserEntity customer = userRepository.findById(customerId).orElse(order.getUser());
        String codRef = "COD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // Check if there is an existing pending payment record to update
        Optional<PaymentEntity> existingPending = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                order.getId(), PaymentStatus.PENDING);

        PaymentEntity payment;
        if (existingPending.isPresent()) {
            payment = existingPending.get();
            payment.setGateway("COD");
            payment.setPaymentMethod("CASH");
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentReference(codRef);
            payment.setUpdatedBy(customer);
        } else {
            payment = PaymentEntity.builder()
                    .order(order)
                    .paymentReference(codRef)
                    .gateway("COD")
                    .paymentMethod("CASH")
                    .amount(order.getGrandTotal())
                    .currency("INR")
                    .status(PaymentStatus.PENDING)
                    .createdBy(customer)
                    .updatedBy(customer)
                    .build();
        }

        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .payment(payment)
                .transactionType(TransactionType.PAYMENT)
                .amount(order.getGrandTotal())
                .status(TransactionStatus.PENDING)
                .createdBy(customer)
                .updatedBy(customer)
                .build();
        payment.addTransaction(tx);

        // Confirm order if PENDING (COD orders are confirmed upon checkout without advance payment)
        if (order.getStatus() == OrderStatus.PENDING) {
            order = orderStatusTransitionService.transitionOrderStatus(
                    order, OrderStatus.CONFIRMED, "Order confirmed with Cash on Delivery", customer);

            List<SellerOrderEntity> sellerOrders = sellerOrderRepository.findByOrderId(order.getId());
            for (SellerOrderEntity sellerOrder : sellerOrders) {
                if (sellerOrder.getStatus() == SellerOrderStatus.PENDING) {
                    orderStatusTransitionService.transitionSellerOrderStatus(
                            sellerOrder, SellerOrderStatus.CONFIRMED, "Order confirmed with COD", customer);
                }
            }
        }

        PaymentEntity savedPayment = paymentRepository.save(payment);
        log.info("[COD SELECTED] Payment record {} created for order {}", savedPayment.getPaymentReference(), order.getOrderNumber());

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse collectCodPayment(Long adminId, Long paymentId, CollectCodPaymentRequest request) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (!"COD".equalsIgnoreCase(payment.getGateway())) {
            throw new InvalidPaymentOperationException("Payment " + payment.getPaymentReference() + " is not a Cash on Delivery transaction");
        }

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            log.info("[COD COLLECT IDEMPOTENT] Payment {} is already marked as collected", payment.getPaymentReference());
            return paymentMapper.toResponse(payment);
        }

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UnauthorizedPaymentAccessException("Admin user not found"));

        BigDecimal collectedAmount = (request != null && request.getAmount() != null) ? request.getAmount() : payment.getAmount();

        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaidAt(Instant.now());
        payment.setUpdatedBy(admin);

        String refNote = (request != null && request.getTransactionReference() != null)
                ? request.getTransactionReference()
                : "COD cash collected by Admin ID " + adminId;

        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .payment(payment)
                .gatewayTransactionId(refNote)
                .transactionType(TransactionType.PAYMENT)
                .amount(collectedAmount)
                .status(TransactionStatus.SUCCESS)
                .processedAt(Instant.now())
                .createdBy(admin)
                .updatedBy(admin)
                .build();
        payment.addTransaction(tx);

        OrderEntity order = payment.getOrder();
        order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID);
        order.setUpdatedBy(admin);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        PaymentEntity savedPayment = paymentRepository.save(payment);
        log.info("[COD COLLECTED] Payment {} marked as collected for order {}", savedPayment.getPaymentReference(), order.getOrderNumber());

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, Long requesterId, boolean isAdmin) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (!isAdmin && !payment.getOrder().getUser().getId().equals(requesterId)) {
            throw new UnauthorizedPaymentAccessException("You do not have access to payment: " + paymentId);
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Long requesterId, boolean isAdmin) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(requesterId)) {
            throw new UnauthorizedPaymentAccessException("You do not have access to payments for order: " + orderId);
        }

        PaymentEntity payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + order.getOrderNumber()));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentSummaryResponse> getPaymentsForCustomer(Long customerId, Pageable pageable) {
        return paymentRepository.findByOrderUserId(customerId, pageable).map(paymentMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentSummaryResponse> getAllPaymentsForAdmin(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(paymentMapper::toSummary);
    }
}
