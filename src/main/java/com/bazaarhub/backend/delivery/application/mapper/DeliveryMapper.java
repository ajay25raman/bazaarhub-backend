package com.bazaarhub.backend.delivery.application.mapper;

import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.delivery.application.dto.response.*;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryTrackingEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DeliveryMapper {

    public DeliveryBoyResponse toDeliveryBoyResponse(DeliveryBoyEntity entity) {
        if (entity == null) {
            return null;
        }

        return DeliveryBoyResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .fullName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .email(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .phone(entity.getUser() != null ? entity.getUser().getPhone() : null)
                .vehicleType(entity.getVehicleType())
                .vehicleNumber(entity.getVehicleNumber())
                .verificationStatus(entity.getVerificationStatus())
                .availabilityStatus(entity.getAvailabilityStatus())
                .verifiedAt(entity.getVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ShipmentResponse toShipmentResponse(ShipmentEntity shipment) {
        if (shipment == null) {
            return null;
        }

        SellerOrderEntity sellerOrder = shipment.getSellerOrder();
        OrderEntity order = sellerOrder != null ? sellerOrder.getOrder() : null;
        SellerEntity seller = sellerOrder != null ? sellerOrder.getSeller() : null;

        // Map customer delivery address
        DeliveryAddressInfo deliveryAddress = null;
        boolean isCod = false;
        BigDecimal codAmount = BigDecimal.ZERO;

        if (order != null) {
            AddressEntity snapAddr = order.getShippingAddress();
            BigDecimal lat = snapAddr != null ? snapAddr.getLatitude() : null;
            BigDecimal lng = snapAddr != null ? snapAddr.getLongitude() : null;

            deliveryAddress = DeliveryAddressInfo.builder()
                    .contactName(order.getRecipientName())
                    .phone(order.getRecipientPhone())
                    .addressLine1(order.getAddressLine1())
                    .addressLine2(order.getAddressLine2())
                    .landmark(order.getLandmark())
                    .city(order.getCity())
                    .state(order.getState())
                    .postalCode(order.getPostalCode())
                    .country(order.getCountry())
                    .latitude(lat)
                    .longitude(lng)
                    .build();

            // Determine COD status: if payment status is PENDING and payment method is COD (or null)
            if (order.getPaymentStatus() == com.bazaarhub.backend.order.domain.model.PaymentStatus.PENDING) {
                isCod = true;
                codAmount = sellerOrder != null ? sellerOrder.getTotalAmount() : order.getGrandTotal();
            }
        }

        // Map seller pickup address
        DeliveryAddressInfo pickupAddress = null;
        if (seller != null) {
            pickupAddress = DeliveryAddressInfo.builder()
                    .contactName(seller.getBusinessName())
                    .phone(seller.getBusinessPhone())
                    .addressLine1("Seller Business Hub: " + seller.getBusinessName())
                    .city("Hub City")
                    .state("Hub State")
                    .country("India")
                    .build();
        }

        // Map items
        List<DeliveryItemInfo> items = Collections.emptyList();
        if (sellerOrder != null && sellerOrder.getItems() != null && !sellerOrder.getItems().isEmpty()) {
            items = new ArrayList<>();
            for (OrderItemEntity item : sellerOrder.getItems()) {
                items.add(DeliveryItemInfo.builder()
                        .productName(item.getProductName())
                        .sku(item.getVariantSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build());
            }
        }

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .shipmentNumber(shipment.getShipmentNumber())
                .sellerOrderId(sellerOrder != null ? sellerOrder.getId() : null)
                .sellerOrderNumber(sellerOrder != null ? sellerOrder.getSubOrderNumber() : null)
                .orderId(order != null ? order.getId() : null)
                .orderNumber(order != null ? order.getOrderNumber() : null)
                .carrierName(shipment.getCarrierName())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus())
                .shippedAt(shipment.getShippedAt())
                .estimatedDeliveryAt(shipment.getEstimatedDeliveryAt())
                .deliveredAt(shipment.getDeliveredAt())
                .deliveryNotes(shipment.getDeliveryNotes())
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .items(items)
                .cod(isCod)
                .codAmount(codAmount)
                .build();
    }

    public DeliveryAssignmentResponse toAssignmentResponse(DeliveryAssignmentEntity assignment) {
        if (assignment == null) {
            return null;
        }

        DeliveryBoyEntity boy = assignment.getDeliveryBoy();
        ShipmentEntity shipment = assignment.getShipment();

        return DeliveryAssignmentResponse.builder()
                .id(assignment.getId())
                .shipmentId(shipment != null ? shipment.getId() : null)
                .shipmentNumber(shipment != null ? shipment.getShipmentNumber() : null)
                .deliveryBoyId(boy != null ? boy.getId() : null)
                .deliveryBoyName(boy != null && boy.getUser() != null ? boy.getUser().getFullName() : null)
                .deliveryBoyPhone(boy != null && boy.getUser() != null ? boy.getUser().getPhone() : null)
                .assignmentStatus(assignment.getAssignmentStatus())
                .assignedAt(assignment.getAssignedAt())
                .acceptedAt(assignment.getAcceptedAt())
                .completedAt(assignment.getCompletedAt())
                .rejectionReason(assignment.getRejectionReason())
                .shipmentDetails(toShipmentResponse(shipment))
                .build();
    }

    public DeliveryTrackingResponse toTrackingResponse(DeliveryTrackingEntity tracking) {
        if (tracking == null) {
            return null;
        }

        return DeliveryTrackingResponse.builder()
                .id(tracking.getId())
                .shipmentId(tracking.getShipment() != null ? tracking.getShipment().getId() : null)
                .deliveryBoyId(tracking.getDeliveryBoy() != null ? tracking.getDeliveryBoy().getId() : null)
                .latitude(tracking.getLatitude())
                .longitude(tracking.getLongitude())
                .recordedAt(tracking.getRecordedAt())
                .build();
    }
}
