package com.bazaarhub.backend.support.application.dto.request;

import com.bazaarhub.backend.support.domain.model.TicketPriority;
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
@Schema(description = "Payload to update the priority level of a support ticket")
public class UpdateTicketPriorityRequest {

    @NotNull(message = "Ticket priority is required")
    @Schema(description = "Target priority (LOW, MEDIUM, HIGH, URGENT)", example = "URGENT")
    private TicketPriority priority;
}
