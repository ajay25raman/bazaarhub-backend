package com.bazaarhub.backend.shipping.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to cancel an active shipment")
public class CancelShipmentRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Schema(description = "Reason for cancelling shipment", example = "Customer requested order cancellation before dispatch")
    private String reason;
}
