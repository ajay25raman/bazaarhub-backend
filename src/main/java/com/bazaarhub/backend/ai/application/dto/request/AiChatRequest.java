package com.bazaarhub.backend.ai.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to interact with BazaarHub AI Shopping & Support Assistant")
public class AiChatRequest {

    @NotBlank(message = "Query message is required")
    @Size(max = 2000, message = "Query message must not exceed 2000 characters")
    @Schema(description = "User query, support inquiry, or order status question", example = "Where is my order ORD-20261115-A1B2C3?")
    private String message;

    @Schema(description = "Optional order ID to verify and retrieve real-time fulfillment status for authenticated customer", example = "10")
    private Long orderId;

    @Schema(description = "Optional existing conversation ID to continue conversation context", example = "5")
    private Long conversationId;
}
