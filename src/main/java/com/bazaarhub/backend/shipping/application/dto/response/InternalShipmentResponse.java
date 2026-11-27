package com.bazaarhub.backend.shipping.application.dto.response;

import com.bazaarhub.backend.delivery.application.dto.response.DeliveryAddressInfo;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryItemInfo;
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
@Schema(description = "Detailed response representing an internal logistics shipment")
public class InternalShipmentResponse {

    @Schema(description = "Shipment ID", example = "10")
    private Long id;

    @Schema(description = "Unique shipment number", example = "SHP-A7BC3D4E5F")
    private String shipmentNumber;

    @Schema(description = "Unique tracking number", example = "BZH-20261114-A7BC3D")
    private String trackingNumber;

    @Schema(description = "Logistics carrier name", example = "BazaarHub Express")
    private String carrierName;

    @Schema(description = "Current lifecycle status", example = "READY_FOR_PICKUP")
    private ShipmentStatus status;

    @Schema(description = "Seller sub-order ID", example = "101")
    private Long sellerOrderId;

    @Schema(description = "Seller sub-order number", example = "SO-ORD-001-S01")
    private String subOrderNumber;

    @Schema(description = "Parent customer order ID", example = "42")
    private Long orderId;

    @Schema(description = "Parent customer order number", example = "ORD-20261114-001")
    private String orderNumber;

    @Schema(description = "Seller ID", example = "5")
    private Long sellerId;

    @Schema(description = "Store name", example = "Apex Retailers")
    private String storeName;

    @Schema(description = "Customer user ID", example = "8")
    private Long customerId;

    @Schema(description = "Customer recipient name", example = "Ajay Kumar")
    private String recipientName;

    @Schema(description = "Customer recipient phone", example = "+91 9876543210")
    private String recipientPhone;

    @Schema(description = "Delivery destination address")
    private DeliveryAddressInfo deliveryAddress;

    @Schema(description = "Store pickup address")
    private DeliveryAddressInfo pickupAddress;

    @Schema(description = "Package details and specifications")
    private PackageInfoResponse packageInfo;

    @Schema(description = "Items included in this shipment")
    private List<DeliveryItemInfo> items;

    @Schema(description = "Shipping charge for this shipment", example = "40.00")
    private BigDecimal shippingAmount;

    @Schema(description = "Parent order payment status", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Payment method (e.g. RAZORPAY or COD)", example = "COD")
    private String paymentMethod;

    @Schema(description = "Actual dispatch / pickup timestamp", example = "2026-11-14T10:00:00Z")
    private Instant shippedAt;

    @Schema(description = "Estimated delivery timestamp", example = "2026-11-16T18:00:00Z")
    private Instant estimatedDeliveryAt;

    @Schema(description = "Actual delivery completion timestamp", example = "2026-11-16T14:30:00Z")
    private Instant deliveredAt;

    @Schema(description = "Delivery notes and logs", example = "Package ready at Store Counter 1")
    private String deliveryNotes;

    @Schema(description = "Shipment record creation timestamp", example = "2026-11-14T08:30:00Z")
    private Instant createdAt;

    @Schema(description = "Shipment record last update timestamp", example = "2026-11-14T09:15:00Z")
    private Instant updatedAt;
}
