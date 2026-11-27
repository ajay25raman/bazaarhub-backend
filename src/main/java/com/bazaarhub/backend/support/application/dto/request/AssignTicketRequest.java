package com.bazaarhub.backend.support.application.dto.request;

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
@Schema(description = "Payload to assign or reassign a support ticket to an agent")
public class AssignTicketRequest {

    @NotNull(message = "Assigned user ID is required")
    @Schema(description = "User ID of the support agent or administrator handling the ticket", example = "5")
    private Long assignedToUserId;
}
