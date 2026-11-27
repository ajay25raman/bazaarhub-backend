package com.bazaarhub.backend.payment.infrastructure.gateway;

import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayOrderResponse;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayPaymentDetails;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayRefundResponse;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {

    /**
     * Creates a server-side order with Razorpay in paise.
     */
    RazorpayOrderResponse createRazorpayOrder(String receipt, BigDecimal amount, String currency, Map<String, String> notes);

    /**
     * Verifies the client checkout HMAC-SHA256 signature.
     * signature = hmac_sha256(orderId + "|" + paymentId, secret)
     */
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    /**
     * Verifies server-to-server webhook HMAC-SHA256 signature using raw payload bytes/string.
     * signature = hmac_sha256(rawBody, webhookSecret)
     */
    boolean verifyWebhookSignature(String rawBody, String signature);

    /**
     * Initiates a refund with Razorpay.
     */
    RazorpayRefundResponse initiateRefund(String gatewayPaymentId, BigDecimal amount, String reason, String receipt);

    /**
     * Fetches real-time payment details from Razorpay to independently verify captured status.
     */
    RazorpayPaymentDetails fetchPaymentDetails(String gatewayPaymentId);

    /**
     * Returns the public Razorpay Key ID for client checkout.
     */
    String getPublicKeyId();
}
