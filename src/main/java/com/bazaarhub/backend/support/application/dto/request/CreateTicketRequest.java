package com.bazaarhub.backend.support.application.dto.request;

import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to submit a new support ticket")
public class CreateTicketRequest {

    @NotNull(message = "Ticket category is required")
    @Schema(description = "Classification category of the support request", example = "ORDER_ISSUE")
    private TicketCategory category;

    @NotBlank(message = "Ticket subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    @Schema(description = "Brief summary of the issue", example = "Order package damaged upon delivery")
    private String subject;

    @NotBlank(message = "Ticket description is required")
    @Schema(description = "Detailed explanation of the issue or inquiry", example = "The outer box was opened and the headphones have a crack.")
    private String description;

    @Builder.Default
    @Schema(description = "Priority level of the ticket", example = "HIGH")
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Schema(description = "Associated order ID if inquiry relates to a specific purchase", example = "105")
    private Long orderId;

    @Size(max = 500, message = "Attachment URL must not exceed 500 characters")
    @Schema(description = "Optional Cloudinary uploaded image or document URL", example = "https://res.cloudinary.com/demo/image/upload/v1/support/proof.jpg")
    private String attachmentUrl;
}
