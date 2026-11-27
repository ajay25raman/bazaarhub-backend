package com.bazaarhub.backend.payment.application.service;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.payment.application.dto.request.CreateRefundRequest;
import com.bazaarhub.backend.payment.application.dto.response.RefundResponse;
import com.bazaarhub.backend.payment.application.mapper.PaymentMapper;
import com.bazaarhub.backend.payment.domain.exception.InvalidRefundOperationException;
import com.bazaarhub.backend.payment.domain.exception.PaymentNotFoundException;
import com.bazaarhub.backend.payment.domain.exception.RefundNotFoundException;
import com.bazaarhub.backend.payment.domain.exception.UnauthorizedPaymentAccessException;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.domain.model.RefundStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionType;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayRefundResponse;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.RefundEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataRefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);

    private final SpringDataRefundRepository refundRepository;
    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataOrderRepository orderRepository;
    private final SpringDataUserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;

    public RefundServiceImpl(SpringDataRefundRepository refundRepository,
                             SpringDataPaymentRepository paymentRepository,
                             SpringDataOrderRepository orderRepository,
                             SpringDataUserRepository userRepository,
                             PaymentGateway paymentGateway,
                             PaymentMapper paymentMapper) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public RefundResponse createRefund(Long adminId, Long paymentId, CreateRefundRequest request) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.CAPTURED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidRefundOperationException(String.format(
                    "Cannot issue refund for payment %s in status %s. Only CAPTURED or PARTIALLY_REFUNDED payments are refundable.",
                    payment.getPaymentReference(), payment.getStatus()));
        }

        BigDecimal alreadyRefunded = refundRepository.getTotalCommittedRefundAmount(paymentId);
        BigDecimal maxRefundable = payment.getAmount().subtract(alreadyRefunded);

        if (request.getRefundAmount().compareTo(maxRefundable) > 0) {
            throw new InvalidRefundOperationException(String.format(
                    "Requested refund amount ₹%s exceeds the maximum available refundable balance of ₹%s (Paid: ₹%s, Already refunded: ₹%s)",
                    request.getRefundAmount(), maxRefundable, payment.getAmount(), alreadyRefunded));
        }

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UnauthorizedPaymentAccessException("Admin user not found"));

        String gatewayRefundId = null;
        if ("RAZORPAY".equalsIgnoreCase(payment.getGateway())) {
            if (payment.getGatewayPaymentId() == null || payment.getGatewayPaymentId().isBlank()) {
                throw new InvalidRefundOperationException("Missing Razorpay payment ID for payment: " + payment.getPaymentReference());
            }

            String receipt = "RFND-" + payment.getOrder().getOrderNumber();
            RazorpayRefundResponse razorpayRefund = paymentGateway.initiateRefund(
                    payment.getGatewayPaymentId(), request.getRefundAmount(), request.getReason(), receipt);
            gatewayRefundId = razorpayRefund.getId();
        } else {
            // Cash on delivery manual refund tracking
            gatewayRefundId = "COD-RFND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        }

        String refundRef = "RFND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        RefundEntity refund = RefundEntity.builder()
                .order(payment.getOrder())
                .payment(payment)
                .requestedBy(admin)
                .refundReference(refundRef)
                .gatewayRefundId(gatewayRefundId)
                .refundAmount(request.getRefundAmount())
                .reason(request.getReason())
                .status(RefundStatus.COMPLETED)
                .processedBy(admin)
                .processedAt(Instant.now())
                .createdBy(admin)
                .updatedBy(admin)
                .build();

        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .payment(payment)
                .gatewayTransactionId(gatewayRefundId)
                .transactionType(TransactionType.REFUND)
                .amount(request.getRefundAmount())
                .status(TransactionStatus.SUCCESS)
                .processedAt(Instant.now())
                .createdBy(admin)
                .updatedBy(admin)
                .build();
        payment.addTransaction(tx);

        BigDecimal totalRefunded = alreadyRefunded.add(request.getRefundAmount());
        OrderEntity order = payment.getOrder();

        if (totalRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PARTIALLY_REFUNDED);
        }

        payment.setUpdatedBy(admin);
        payment.setUpdatedAt(Instant.now());
        order.setUpdatedBy(admin);
        order.setUpdatedAt(Instant.now());

        RefundEntity savedRefund = refundRepository.save(refund);
        paymentRepository.save(payment);
        orderRepository.save(order);

        log.info("[REFUND PROCESSED] Refund {} of ₹{} executed for payment {}",
                savedRefund.getRefundReference(), savedRefund.getRefundAmount(), payment.getPaymentReference());

        return paymentMapper.toRefundResponse(savedRefund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundById(Long refundId, Long requesterId, boolean isAdmin) {
        RefundEntity refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException("Refund not found with ID: " + refundId));

        if (!isAdmin && !refund.getOrder().getUser().getId().equals(requesterId)) {
            throw new UnauthorizedPaymentAccessException("You do not have access to refund: " + refundId);
        }

        return paymentMapper.toRefundResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsForPayment(Long paymentId, Long requesterId, boolean isAdmin) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (!isAdmin && !payment.getOrder().getUser().getId().equals(requesterId)) {
            throw new UnauthorizedPaymentAccessException("You do not have access to refunds for payment: " + paymentId);
        }

        return refundRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                .map(paymentMapper::toRefundResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundResponse> getAllRefundsForAdmin(Pageable pageable) {
        return refundRepository.findAll(pageable).map(paymentMapper::toRefundResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundResponse> getRefundsForCustomer(Long customerId, Pageable pageable) {
        return refundRepository.findByOrderUserId(customerId, pageable).map(paymentMapper::toRefundResponse);
    }
}
