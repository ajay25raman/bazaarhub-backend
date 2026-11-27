package com.bazaarhub.backend.order.application.dto.response;

import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Summary representation of an order for list views")
public class OrderSummaryResponse {

    @Schema(description = "Order ID", example = "10")
    private Long id;

    @Schema(description = "Order reference number", example = "ORD-20261114-A8F12B90")
    private String orderNumber;

    @Schema(description = "Customer ID", example = "3")
    private Long customerId;

    @Schema(description = "Customer Name", example = "Ajay Kumar")
    private String customerName;

    @Schema(description = "Order status", example = "CONFIRMED")
    private OrderStatus status;

    @Schema(description = "Payment status", example = "PENDING")
    private PaymentStatus paymentStatus;

    @Schema(description = "Grand total", example = "129999.00")
    private BigDecimal grandTotal;

    @Schema(description = "Total number of items in order", example = "2")
    private int totalItems;

    @Schema(description = "Number of distinct seller sub-orders", example = "1")
    private int sellerOrdersCount;

    @Schema(description = "Order creation timestamp")
    private Instant createdAt;
}
