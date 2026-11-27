package com.bazaarhub.backend.delivery.application.dto.response;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed shipment information for customer, seller, or delivery boy")
public class ShipmentResponse {

    @Schema(description = "Shipment ID", example = "101")
    private Long id;

    @Schema(description = "Unique shipment number", example = "SHP-7A8B9C0D")
    private String shipmentNumber;

    @Schema(description = "Seller Sub-Order ID", example = "42")
    private Long sellerOrderId;

    @Schema(description = "Seller Sub-Order Number", example = "SO-12345678-01")
    private String sellerOrderNumber;

    @Schema(description = "Parent Order ID", example = "15")
    private Long orderId;

    @Schema(description = "Parent Order Number", example = "ORD-20261114-12345")
    private String orderNumber;

    @Schema(description = "Carrier name", example = "BazaarHub Fleet")
    private String carrierName;

    @Schema(description = "Tracking number", example = "TRK-987654321")
    private String trackingNumber;

    @Schema(description = "Shipment lifecycle status", example = "OUT_FOR_DELIVERY")
    private ShipmentStatus status;

    @Schema(description = "Timestamp when marked as shipped / picked up")
    private Instant shippedAt;

    @Schema(description = "Estimated delivery target timestamp")
    private Instant estimatedDeliveryAt;

    @Schema(description = "Actual delivery completion timestamp")
    private Instant deliveredAt;

    @Schema(description = "Operational delivery notes, proof metadata, or remarks", example = "Handed to recipient")
    private String deliveryNotes;

    @Schema(description = "Seller pickup address")
    private DeliveryAddressInfo pickupAddress;

    @Schema(description = "Customer drop-off delivery address")
    private DeliveryAddressInfo deliveryAddress;

    @Schema(description = "Parcel item contents")
    private List<DeliveryItemInfo> items;

    @Schema(description = "Indicates whether Cash on Delivery is required", example = "true")
    private boolean cod;

    @Schema(description = "Amount to collect if Cash on Delivery", example = "1499.00")
    private BigDecimal codAmount;
}
