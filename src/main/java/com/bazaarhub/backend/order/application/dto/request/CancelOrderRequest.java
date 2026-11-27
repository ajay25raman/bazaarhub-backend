package com.bazaarhub.backend.order.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to cancel an order or seller sub-order")
public class CancelOrderRequest {

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    @Schema(description = "Reason for cancellation", example = "Ordered by mistake, changed mind.")
    private String reason;
}
