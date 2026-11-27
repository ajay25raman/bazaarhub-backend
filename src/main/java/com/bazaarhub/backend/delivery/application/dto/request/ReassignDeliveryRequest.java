package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to reassign an active delivery task to another delivery partner")
public class ReassignDeliveryRequest {

    @NotNull(message = "New delivery boy ID is required")
    @Schema(description = "New delivery boy ID to assign", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long newDeliveryBoyId;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(description = "Reason for reassignment", example = "Original partner bike breakdown")
    private String reason;
}
