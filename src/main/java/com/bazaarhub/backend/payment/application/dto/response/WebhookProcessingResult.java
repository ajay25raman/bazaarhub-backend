package com.bazaarhub.backend.payment.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Result DTO returned upon processing a Razorpay webhook event.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Acknowledgment payload for processed Razorpay webhook event")
public class WebhookProcessingResult {

    @Schema(description = "Indicates whether webhook was accepted and processed", example = "true")
    private boolean success;

    @Schema(description = "Event type extracted from payload", example = "payment.captured")
    private String event;

    @Schema(description = "Summary message of processing action", example = "Payment captured and order confirmed successfully")
    private String message;

    @Schema(description = "Matched internal payment reference", example = "PAY-A1B2C3D4E5F6")
    private String paymentReference;
}
