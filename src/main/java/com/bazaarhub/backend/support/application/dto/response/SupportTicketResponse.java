package com.bazaarhub.backend.support.application.dto.response;

import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketPriority;
import com.bazaarhub.backend.support.domain.model.TicketStatus;
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
@Schema(description = "Representation of a support ticket")
public class SupportTicketResponse {

    @Schema(description = "Unique ticket ID", example = "10")
    private Long id;

    @Schema(description = "Unique alphanumeric human-readable ticket tracking number", example = "TKT-20261115-8F3A")
    private String ticketNumber;

    @Schema(description = "User ID who created the ticket", example = "1")
    private Long userId;

    @Schema(description = "Full name of the ticket creator", example = "Ajay Kumar")
    private String userFullName;

    @Schema(description = "Email of the ticket creator", example = "ajay@example.com")
    private String userEmail;

    @Schema(description = "Associated order ID", example = "105")
    private Long orderId;

    @Schema(description = "Associated order number", example = "ORD-20261115-ABC")
    private String orderNumber;

    @Schema(description = "Ticket category", example = "ORDER_ISSUE")
    private TicketCategory category;

    @Schema(description = "Ticket subject headline", example = "Package damaged on delivery")
    private String subject;

    @Schema(description = "Full issue description", example = "The outer box was opened and headphones cracked.")
    private String description;

    @Schema(description = "Ticket priority", example = "HIGH")
    private TicketPriority priority;

    @Schema(description = "Current lifecycle status", example = "OPEN")
    private TicketStatus status;

    @Schema(description = "Assigned support agent user ID", example = "5")
    private Long assignedToId;

    @Schema(description = "Assigned support agent name", example = "Support Agent Priya")
    private String assignedToName;

    @Schema(description = "Timestamp when ticket was marked resolved", example = "2026-11-15T09:30:00Z")
    private Instant resolvedAt;

    @Schema(description = "Timestamp when ticket was closed", example = "2026-11-15T10:00:00Z")
    private Instant closedAt;

    @Schema(description = "Creation timestamp", example = "2026-11-15T08:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-15T08:30:00Z")
    private Instant updatedAt;

    @Schema(description = "Total number of messages in the ticket conversation", example = "4")
    private long messageCount;
}
