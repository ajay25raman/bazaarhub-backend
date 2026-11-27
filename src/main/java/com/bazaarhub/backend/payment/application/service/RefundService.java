package com.bazaarhub.backend.payment.application.service;

import com.bazaarhub.backend.payment.application.dto.request.CreateRefundRequest;
import com.bazaarhub.backend.payment.application.dto.response.RefundResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for refund initiation, execution, and audit queries.
 */
public interface RefundService {

    /**
     * Initiates a refund against a captured payment through Razorpay (or manual for COD).
     */
    RefundResponse createRefund(Long adminId, Long paymentId, CreateRefundRequest request);

    /**
     * Retrieves refund details by ID with permission checks.
     */
    RefundResponse getRefundById(Long refundId, Long requesterId, boolean isAdmin);

    /**
     * Lists all refunds associated with a specific payment.
     */
    List<RefundResponse> getRefundsForPayment(Long paymentId, Long requesterId, boolean isAdmin);

    /**
     * Lists all refunds in the system for administrators.
     */
    Page<RefundResponse> getAllRefundsForAdmin(Pageable pageable);

    /**
     * Lists refunds for orders belonging to an authenticated customer.
     */
    Page<RefundResponse> getRefundsForCustomer(Long customerId, Pageable pageable);
}
