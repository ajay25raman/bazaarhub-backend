package com.bazaarhub.backend.order.application.dto.response;

import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
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
@Schema(description = "Seller sub-order response")
public class SellerOrderResponse {

    @Schema(description = "Seller sub-order ID", example = "25")
    private Long id;

    @Schema(description = "Parent order ID", example = "10")
    private Long orderId;

    @Schema(description = "Parent order reference number", example = "ORD-20261114-A8F12B90")
    private String orderNumber;

    @Schema(description = "Seller ID", example = "2")
    private Long sellerId;

    @Schema(description = "Seller business name", example = "Apex Retailers Pvt Ltd")
    private String sellerBusinessName;

    @Schema(description = "Sub-order reference number", example = "SUB-20261114-A8F12B90-S2")
    private String subOrderNumber;

    @Schema(description = "Sub-order status", example = "CONFIRMED")
    private SellerOrderStatus status;

    @Schema(description = "Subtotal for this seller's items", example = "129999.00")
    private BigDecimal subtotal;

    @Schema(description = "Tax amount for this seller's items", example = "23399.82")
    private BigDecimal taxAmount;

    @Schema(description = "Delivery fee allocated to this seller", example = "0.00")
    private BigDecimal deliveryFee;

    @Schema(description = "Total amount for this seller sub-order", example = "129999.00")
    private BigDecimal totalAmount;

    @Schema(description = "Timestamp when sub-order was cancelled, if applicable")
    private Instant cancelledAt;

    @Schema(description = "Cancellation reason, if applicable")
    private String cancellationReason;

    @Schema(description = "Customer delivery address")
    private AddressResponse shippingAddress;

    @Schema(description = "Sub-order creation timestamp")
    private Instant createdAt;

    @Schema(description = "Sub-order last updated timestamp")
    private Instant updatedAt;

    @Schema(description = "List of ordered items for this seller")
    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();

    @Schema(description = "Audit trail of status transitions for this seller order")
    @Builder.Default
    private List<OrderStatusHistoryResponse> statusHistory = new ArrayList<>();
}
