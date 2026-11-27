package com.bazaarhub.backend.support.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representation of a message within a support ticket conversation")
public class SupportMessageResponse {

    @Schema(description = "Unique message ID", example = "42")
    private Long id;

    @Schema(description = "Ticket ID", example = "10")
    private Long ticketId;

    @Schema(description = "Sender user ID", example = "1")
    private Long senderId;

    @Schema(description = "Full name of the sender", example = "Ajay Kumar")
    private String senderName;

    @Schema(description = "Sender email address", example = "ajay@example.com")
    private String senderEmail;

    @Schema(description = "Message body content", example = "Here is the photo of the damaged package.")
    private String message;

    @Schema(description = "Cloudinary public attachment URL if an image or document was attached", example = "https://res.cloudinary.com/demo/image/upload/v1/support/proof.jpg")
    private String attachmentUrl;

    @com.fasterxml.jackson.annotation.JsonProperty("isInternal")
    @Schema(description = "Whether this is an internal staff note invisible to customers", example = "false")
    private boolean isInternal;

    @Schema(description = "Timestamp when message was sent", example = "2026-11-15T08:15:00Z")
    private Instant createdAt;
}
