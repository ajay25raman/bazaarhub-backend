package com.bazaarhub.backend.cart.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation response verifying checkout eligibility before order creation")
public class CheckoutValidationResponse {

    @Schema(description = "Whether the checkout criteria are strictly valid", example = "true")
    private boolean valid;

    @Builder.Default
    @Schema(description = "List of validation errors preventing checkout")
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of advisory warnings (e.g., price updates or stock warnings)")
    private List<String> warnings = new ArrayList<>();

    @Schema(description = "Cart ID", example = "1")
    private Long cartId;

    @Schema(description = "Validated Address ID (null if not provided)", example = "5")
    private Long addressId;

    @Schema(description = "Current subtotal", example = "379800.00")
    private BigDecimal subtotal;

    @Schema(description = "Grand total payable", example = "379800.00")
    private BigDecimal grandTotal;
}
