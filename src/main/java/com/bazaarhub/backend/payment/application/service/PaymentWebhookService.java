package com.bazaarhub.backend.payment.application.service;

import com.bazaarhub.backend.payment.application.dto.response.WebhookProcessingResult;

/**
 * Service interface for processing asynchronous server-to-server Razorpay webhook events.
 */
public interface PaymentWebhookService {

    /**
     * Validates the webhook signature against the raw payload and applies idempotent state updates.
     *
     * @param rawPayload Raw JSON byte string as delivered by Razorpay
     * @param signatureHeader Signature passed in X-Razorpay-Signature HTTP header
     * @return WebhookProcessingResult describing actions taken
     */
    WebhookProcessingResult processWebhook(String rawPayload, String signatureHeader);
}
