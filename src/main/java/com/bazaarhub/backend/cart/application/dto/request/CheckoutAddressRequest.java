package com.bazaarhub.backend.cart.application.dto.request;

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
@Schema(description = "Request to select a customer delivery address for checkout")
public class CheckoutAddressRequest {

    @NotNull(message = "Address ID is required")
    @Schema(description = "ID of the customer's verified delivery address", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long addressId;
}
