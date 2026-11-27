package com.bazaarhub.backend.order.application.dto.response;

import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Full customer order response")
public class OrderResponse {

    @Schema(description = "Order ID", example = "10")
    private Long id;

    @Schema(description = "Unique human-readable order number", example = "ORD-20261114-A8F12B90")
    private String orderNumber;

    @Schema(description = "Customer ID", example = "3")
    private Long customerId;

    @Schema(description = "Customer full name", example = "Ajay Kumar")
    private String customerName;

    @Schema(description = "Customer email", example = "customer@bazaarhub.com")
    private String customerEmail;

    @Schema(description = "Shipping delivery address")
    private AddressResponse shippingAddress;

    @Schema(description = "Overall order status", example = "CONFIRMED")
    private OrderStatus status;

    @Schema(description = "Payment status", example = "PENDING")
    private PaymentStatus paymentStatus;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Subtotal before tax, delivery and discounts", example = "129999.00")
    private BigDecimal subtotal;

    @Schema(description = "Total tax amount across all items", example = "23399.82")
    private BigDecimal taxAmount;

    @Schema(description = "Delivery fee", example = "0.00")
    private BigDecimal deliveryFee;

    @Schema(description = "Discount amount applied", example = "0.00")
    private BigDecimal discountAmount;

    @Schema(description = "Grand total payable for the order", example = "129999.00")
    private BigDecimal grandTotal;

    @Schema(description = "Special delivery instructions", example = "Please leave package at the front door.")
    private String specialInstructions;

    @Schema(description = "Cancellation timestamp, if order is cancelled")
    private Instant cancelledAt;

    @Schema(description = "Reason provided for order cancellation")
    private String cancellationReason;

    @Schema(description = "Order creation timestamp")
    private Instant createdAt;

    @Schema(description = "Order last updated timestamp")
    private Instant updatedAt;

    @Schema(description = "List of vendor sub-orders comprising this order")
    @Builder.Default
    private List<SellerOrderResponse> sellerOrders = new ArrayList<>();

    @Schema(description = "Order lifecycle transition audit history")
    @Builder.Default
    private List<OrderStatusHistoryResponse> statusHistory = new ArrayList<>();
}
