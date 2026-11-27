package com.bazaarhub.backend.payment.presentation.rest;

import com.bazaarhub.backend.payment.application.dto.response.WebhookProcessingResult;
import com.bazaarhub.backend.payment.application.service.PaymentWebhookService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public webhook receiver for asynchronous Razorpay gateway events.
 * Authenticated cryptographically via X-Razorpay-Signature against raw request payload.
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
@Tag(name = "Payment Webhooks", description = "Asynchronous gateway webhook receivers for automated payment and refund reconciliation")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/razorpay")
    @Operation(summary = "Receive Razorpay webhook event", description = "Processes asynchronous events such as payment.captured, payment.failed, and refund.processed.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid webhook signature or malformed body", content = @Content)
    })
    public ResponseEntity<ApiResponse<WebhookProcessingResult>> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader,
            @RequestBody(required = false) String rawPayload) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Webhook signature header 'X-Razorpay-Signature' is required"));
        }
        if (rawPayload == null || rawPayload.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Webhook payload is required"));
        }
        if (rawPayload.length() > 65536) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Webhook payload exceeds maximum permitted size of 64KB"));
        }
        WebhookProcessingResult result = paymentWebhookService.processWebhook(rawPayload, signatureHeader);
        return ResponseEntity.ok(ApiResponse.ok("Webhook processed", result));
    }
}
