package com.bazaarhub.backend.support.application.dto.request;

import com.bazaarhub.backend.support.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to update the lifecycle status of a support ticket")
public class UpdateTicketStatusRequest {

    @NotNull(message = "Ticket status is required")
    @Schema(description = "Target status (OPEN, IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED, CLOSED)", example = "RESOLVED")
    private TicketStatus status;

    @Schema(description = "Optional resolution summary message sent to the ticket creator", example = "Refund processed successfully. Credit will reflect within 2 business days.")
    private String resolutionNotes;
}
