package com.bazaarhub.backend.payment.application.service;

import com.bazaarhub.backend.payment.application.dto.request.CollectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.CreateRazorpayOrderRequest;
import com.bazaarhub.backend.payment.application.dto.request.SelectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.VerifyRazorpayPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.response.PaymentResponse;
import com.bazaarhub.backend.payment.application.dto.response.PaymentSummaryResponse;
import com.bazaarhub.backend.payment.application.dto.response.RazorpayOrderCheckoutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Core business service interface for payment management (Razorpay and Cash on Delivery).
 */
public interface PaymentService {

    /**
     * Creates or retrieves a pending Razorpay order for customer checkout.
     */
    RazorpayOrderCheckoutResponse createRazorpayOrder(Long customerId, CreateRazorpayOrderRequest request);

    /**
     * Verifies the cryptographic HMAC signature returned by Razorpay Checkout SDK.
     */
    PaymentResponse verifyRazorpayPayment(Long customerId, VerifyRazorpayPaymentRequest request);

    /**
     * Configures an order for Cash on Delivery (COD) settlement.
     */
    PaymentResponse selectCodPayment(Long customerId, SelectCodPaymentRequest request);

    /**
     * Marks a COD payment collected upon delivery (administrative/fulfillment action).
     */
    PaymentResponse collectCodPayment(Long adminId, Long paymentId, CollectCodPaymentRequest request);

    /**
     * Retrieves detailed payment information by ID with ownership enforcement.
     */
    PaymentResponse getPaymentById(Long paymentId, Long requesterId, boolean isAdmin);

    /**
     * Retrieves detailed payment information by Order ID with ownership enforcement.
     */
    PaymentResponse getPaymentByOrderId(Long orderId, Long requesterId, boolean isAdmin);

    /**
     * Lists paginated payment records for the authenticated customer.
     */
    Page<PaymentSummaryResponse> getPaymentsForCustomer(Long customerId, Pageable pageable);

    /**
     * Lists all payment records across all orders for administrators.
     */
    Page<PaymentSummaryResponse> getAllPaymentsForAdmin(Pageable pageable);
}
