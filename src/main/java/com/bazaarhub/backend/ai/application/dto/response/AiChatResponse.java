package com.bazaarhub.backend.ai.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response from BazaarHub AI Shopping & Support Assistant")
public class AiChatResponse {

    @Schema(description = "Conversation session ID", example = "5")
    private Long conversationId;

    @Schema(description = "Assistant response message", example = "Your order ORD-20261115-A1B2C3 is currently Out For Delivery by courier ExpressLogistics.")
    private String reply;

    @Schema(description = "Recommended ticket category if the inquiry requires formal helpdesk resolution", example = "DELIVERY_ISSUE")
    private String suggestedCategory;

    @Schema(description = "Whether the assistant recommends escalating the issue to a human support agent", example = "false")
    private boolean escalateToHuman;

    @Schema(description = "Verified order status summary if queried", example = "Order #ORD-20261115-A1B2C3: Status DELIVERED, 2 items, ₹2,499.00 paid via Razorpay.")
    private String orderStatusSummary;

    @Schema(description = "Provider model that generated this response", example = "openai/gpt-oss-20b")
    private String modelName;
}
