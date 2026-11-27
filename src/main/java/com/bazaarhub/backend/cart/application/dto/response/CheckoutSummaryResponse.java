package com.bazaarhub.backend.cart.application.dto.response;

import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
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
@Schema(description = "Checkout preparation summary containing validated cart, delivery address, and readiness status")
public class CheckoutSummaryResponse {

    @Schema(description = "Whether the customer can proceed to place order and payment", example = "true")
    private boolean canProceedToCheckout;

    @Schema(description = "Checkout readiness status (READY, ACTION_REQUIRED, EMPTY_CART)", example = "READY")
    private String checkoutStatus;

    @Builder.Default
    @Schema(description = "List of validation errors blocking checkout", example = "[]")
    private List<String> validationErrors = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of warnings (e.g. price updates)", example = "[]")
    private List<String> warnings = new ArrayList<>();

    @Schema(description = "Full validated customer cart")
    private CartResponse cart;

    @Schema(description = "Selected delivery address (null if address not yet selected)")
    private AddressResponse selectedAddress;

    @Schema(description = "Total number of items in checkout", example = "3")
    private int itemsCount;

    @Schema(description = "Number of distinct sellers fulfilling this order", example = "2")
    private int sellersCount;

    @Schema(description = "Subtotal payable", example = "379800.00")
    private BigDecimal subtotal;

    @Schema(description = "Computed applicable tax", example = "0.00")
    private BigDecimal taxAmount;

    @Schema(description = "Delivery / shipping charge", example = "0.00")
    private BigDecimal deliveryFee;

    @Schema(description = "Final grand total amount", example = "379800.00")
    private BigDecimal grandTotal;
}
