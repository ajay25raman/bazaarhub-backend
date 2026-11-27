package com.bazaarhub.backend.cart.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to validate checkout readiness before order placement")
public class CheckoutValidateRequest {

    @Positive(message = "Address ID must be positive")
    @Schema(description = "Optional Address ID to validate against customer address book", example = "5")
    private Long addressId;
}
