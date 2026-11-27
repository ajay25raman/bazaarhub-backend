package com.bazaarhub.backend.order.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create an order from current active cart")
public class CreateOrderRequest {

    @Schema(description = "Delivery address ID. If null, the customer's default address will be used.", example = "1")
    private Long addressId;

    @Size(max = 500, message = "Special instructions cannot exceed 500 characters")
    @Schema(description = "Special instructions or delivery notes", example = "Please leave package at the front door.")
    private String specialInstructions;
}
