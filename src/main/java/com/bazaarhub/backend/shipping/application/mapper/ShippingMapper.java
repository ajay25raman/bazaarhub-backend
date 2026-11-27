package com.bazaarhub.backend.shipping.application.mapper;

import com.bazaarhub.backend.delivery.application.dto.response.DeliveryAddressInfo;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryItemInfo;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import com.bazaarhub.backend.shipping.application.dto.response.InternalShipmentResponse;
import com.bazaarhub.backend.shipping.application.dto.response.PackageInfoResponse;
import com.bazaarhub.backend.shipping.application.dto.response.PublicTrackingResponse;
import com.bazaarhub.backend.shipping.domain.model.TrackingMilestone;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ShippingMapper {

    private static final Pattern PKG_WEIGHT_PATTERN = Pattern.compile("weight=([0-9.]+)kg");
    private static final Pattern PKG_DIMS_PATTERN = Pattern.compile("dims=([0-9.]+)x([0-9.]+)x([0-9.]+)cm");
    private static final Pattern PKG_FRAGILE_PATTERN = Pattern.compile("fragile=(true|false)");
    private static final Pattern PKG_PHOTO_PATTERN = Pattern.compile("\\[PKG_PHOTO:\\s*([^\\]]+)\\]");

    public InternalShipmentResponse toInternalResponse(ShipmentEntity entity, String paymentMethod) {
        if (entity == null) {
            return null;
        }

        SellerOrderEntity sellerOrder = entity.getSellerOrder();
        OrderEntity order = sellerOrder != null ? sellerOrder.getOrder() : null;
        SellerEntity seller = sellerOrder != null ? sellerOrder.getSeller() : null;
        StoreEntity store = seller != null ? seller.getStore() : null;

        List<DeliveryItemInfo> items = new ArrayList<>();
        BigDecimal computedWeight = BigDecimal.ZERO;

        if (sellerOrder != null && sellerOrder.getItems() != null) {
            for (OrderItemEntity item : sellerOrder.getItems()) {
                BigDecimal itemWeight = (item.getVariant() != null && item.getVariant().getWeight() != null)
                        ? item.getVariant().getWeight()
                        : BigDecimal.valueOf(0.5);

                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                computedWeight = computedWeight.add(itemWeight.multiply(BigDecimal.valueOf(qty)));

                items.add(DeliveryItemInfo.builder()
                        .productName(item.getProductName())
                        .sku(item.getVariantSku())
                        .quantity(qty)
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build());
            }
        }

        PackageInfoResponse pkgInfo = parsePackageInfo(entity.getDeliveryNotes(), computedWeight);

        return InternalShipmentResponse.builder()
                .id(entity.getId())
                .shipmentNumber(entity.getShipmentNumber())
                .trackingNumber(entity.getTrackingNumber())
                .carrierName(entity.getCarrierName() != null ? entity.getCarrierName() : "BazaarHub Express")
                .status(entity.getStatus())
                .sellerOrderId(sellerOrder != null ? sellerOrder.getId() : null)
                .subOrderNumber(sellerOrder != null ? sellerOrder.getSubOrderNumber() : null)
                .orderId(order != null ? order.getId() : null)
                .orderNumber(order != null ? order.getOrderNumber() : null)
                .sellerId(seller != null ? seller.getId() : null)
                .storeName(store != null ? store.getStoreName() : (seller != null ? seller.getBusinessName() : null))
                .customerId(order != null && order.getUser() != null ? order.getUser().getId() : null)
                .recipientName(order != null ? order.getRecipientName() : null)
                .recipientPhone(order != null ? order.getRecipientPhone() : null)
                .deliveryAddress(buildDeliveryAddress(order))
                .pickupAddress(buildPickupAddress(seller, store))
                .packageInfo(pkgInfo)
                .items(items)
                .shippingAmount(sellerOrder != null ? sellerOrder.getDeliveryFee() : BigDecimal.ZERO)
                .paymentStatus(order != null && order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .paymentMethod(paymentMethod != null ? paymentMethod : "PREPAID")
                .shippedAt(entity.getShippedAt())
                .estimatedDeliveryAt(entity.getEstimatedDeliveryAt())
                .deliveredAt(entity.getDeliveredAt())
                .deliveryNotes(entity.getDeliveryNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PublicTrackingResponse toPublicTrackingResponse(ShipmentEntity entity) {
        if (entity == null) {
            return null;
        }

        SellerOrderEntity sellerOrder = entity.getSellerOrder();
        OrderEntity order = sellerOrder != null ? sellerOrder.getOrder() : null;
        StoreEntity store = (sellerOrder != null && sellerOrder.getSeller() != null)
                ? sellerOrder.getSeller().getStore()
                : null;

        String originCity = store != null ? "Bengaluru" : "Store Hub";
        String destinationCity = order != null ? order.getCity() : null;
        String destinationState = order != null ? order.getState() : null;

        return PublicTrackingResponse.builder()
                .trackingNumber(entity.getTrackingNumber())
                .shipmentNumber(entity.getShipmentNumber())
                .carrierName(entity.getCarrierName() != null ? entity.getCarrierName() : "BazaarHub Express")
                .status(entity.getStatus())
                .originCity(originCity)
                .destinationCity(destinationCity)
                .destinationState(destinationState)
                .shippedAt(entity.getShippedAt())
                .estimatedDeliveryAt(entity.getEstimatedDeliveryAt())
                .deliveredAt(entity.getDeliveredAt())
                .milestones(buildMilestones(entity, originCity, destinationCity))
                .build();
    }

    public List<TrackingMilestone> buildMilestones(ShipmentEntity entity, String originCity, String destCity) {
        List<TrackingMilestone> list = new ArrayList<>();
        ShipmentStatus current = entity.getStatus();

        // 1. Order Placed & Shipment Created
        list.add(TrackingMilestone.builder()
                .status(ShipmentStatus.PENDING)
                .title("Shipment Created")
                .description("Shipping label and dispatch order created")
                .timestamp(entity.getCreatedAt())
                .location(originCity)
                .completed(true)
                .build());

        // 2. Ready for Pickup
        boolean readyReached = isStatusReached(current, ShipmentStatus.READY_FOR_PICKUP);
        list.add(TrackingMilestone.builder()
                .status(ShipmentStatus.READY_FOR_PICKUP)
                .title("Ready for Pickup")
                .description("Seller packaged items and confirmed ready for delivery partner pickup")
                .timestamp(readyReached ? entity.getCreatedAt() : null)
                .location(originCity)
                .completed(readyReached)
                .build());

        // 3. Picked Up
        boolean pickedUpReached = isStatusReached(current, ShipmentStatus.PICKED_UP);
        list.add(TrackingMilestone.builder()
                .status(ShipmentStatus.PICKED_UP)
                .title("Picked Up")
                .description("Package collected by BazaarHub delivery partner from seller store")
                .timestamp(pickedUpReached ? (entity.getShippedAt() != null ? entity.getShippedAt() : entity.getUpdatedAt()) : null)
                .location(originCity)
                .completed(pickedUpReached)
                .build());

        // 4. In Transit
        boolean transitReached = isStatusReached(current, ShipmentStatus.IN_TRANSIT);
        list.add(TrackingMilestone.builder()
                .status(ShipmentStatus.IN_TRANSIT)
                .title("In Transit")
                .description("Package is moving towards destination area")
                .timestamp(transitReached ? entity.getUpdatedAt() : null)
                .location(destCity)
                .completed(transitReached)
                .build());

        // 5. Out for Delivery
        boolean outForDeliveryReached = isStatusReached(current, ShipmentStatus.OUT_FOR_DELIVERY);
        list.add(TrackingMilestone.builder()
                .status(ShipmentStatus.OUT_FOR_DELIVERY)
                .title("Out for Delivery")
                .description("Delivery partner is out for delivery. OTP verification required.")
                .timestamp(outForDeliveryReached ? entity.getUpdatedAt() : null)
                .location(destCity)
                .completed(outForDeliveryReached)
                .build());

        // 6. Delivered / Failed / Cancelled
        if (current == ShipmentStatus.DELIVERED) {
            list.add(TrackingMilestone.builder()
                    .status(ShipmentStatus.DELIVERED)
                    .title("Delivered")
                    .description("Package successfully delivered to customer")
                    .timestamp(entity.getDeliveredAt() != null ? entity.getDeliveredAt() : entity.getUpdatedAt())
                    .location(destCity)
                    .completed(true)
                    .build());
        } else if (current == ShipmentStatus.FAILED) {
            list.add(TrackingMilestone.builder()
                    .status(ShipmentStatus.FAILED)
                    .title("Delivery Attempt Failed")
                    .description("Delivery attempt unsuccessful: " + (entity.getDeliveryNotes() != null ? entity.getDeliveryNotes() : "Customer unavailable"))
                    .timestamp(entity.getUpdatedAt())
                    .location(destCity)
                    .completed(true)
                    .build());
        } else if (current == ShipmentStatus.CANCELLED) {
            list.add(TrackingMilestone.builder()
                    .status(ShipmentStatus.CANCELLED)
                    .title("Cancelled")
                    .description("Shipment was cancelled")
                    .timestamp(entity.getUpdatedAt())
                    .location(originCity)
                    .completed(true)
                    .build());
        } else {
            list.add(TrackingMilestone.builder()
                    .status(ShipmentStatus.DELIVERED)
                    .title("Delivery Pending")
                    .description("Package expected by " + (entity.getEstimatedDeliveryAt() != null ? entity.getEstimatedDeliveryAt().toString() : "soon"))
                    .timestamp(null)
                    .location(destCity)
                    .completed(false)
                    .build());
        }

        return list;
    }

    private boolean isStatusReached(ShipmentStatus current, ShipmentStatus target) {
        if (current == null) return false;
        if (current == ShipmentStatus.CANCELLED) return false;

        List<ShipmentStatus> flow = List.of(
                ShipmentStatus.PENDING,
                ShipmentStatus.READY_FOR_PICKUP,
                ShipmentStatus.PICKED_UP,
                ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY,
                ShipmentStatus.DELIVERED
        );

        int currentIndex = flow.indexOf(current);
        int targetIndex = flow.indexOf(target);

        if (currentIndex < 0) {
            // E.g. FAILED or RETURNED
            return target == ShipmentStatus.PENDING ||
                   target == ShipmentStatus.READY_FOR_PICKUP ||
                   target == ShipmentStatus.PICKED_UP;
        }

        return currentIndex >= targetIndex;
    }

    public PackageInfoResponse parsePackageInfo(String notes, BigDecimal fallbackWeight) {
        BigDecimal weight = fallbackWeight != null && fallbackWeight.compareTo(BigDecimal.ZERO) > 0
                ? fallbackWeight
                : BigDecimal.valueOf(0.5);
        BigDecimal length = BigDecimal.valueOf(20.0);
        BigDecimal width = BigDecimal.valueOf(15.0);
        BigDecimal height = BigDecimal.valueOf(10.0);
        boolean fragile = false;
        String cleanNotes = notes;
        String photoUrl = null;

        if (notes != null && !notes.isBlank()) {
            Matcher wm = PKG_WEIGHT_PATTERN.matcher(notes);
            if (wm.find()) {
                try {
                    weight = new BigDecimal(wm.group(1));
                } catch (Exception ignored) {}
            }

            Matcher dm = PKG_DIMS_PATTERN.matcher(notes);
            if (dm.find()) {
                try {
                    length = new BigDecimal(dm.group(1));
                    width = new BigDecimal(dm.group(2));
                    height = new BigDecimal(dm.group(3));
                } catch (Exception ignored) {}
            }

            Matcher fm = PKG_FRAGILE_PATTERN.matcher(notes);
            if (fm.find()) {
                fragile = Boolean.parseBoolean(fm.group(1));
            }

            Matcher pm = PKG_PHOTO_PATTERN.matcher(notes);
            if (pm.find()) {
                photoUrl = pm.group(1);
            }
        }

        return PackageInfoResponse.builder()
                .weightKg(weight)
                .lengthCm(length)
                .widthCm(width)
                .heightCm(height)
                .fragile(fragile)
                .packageQuantity(1)
                .notes(cleanNotes)
                .conditionPhotoUrl(photoUrl)
                .build();
    }

    public String encodePackageInfo(BigDecimal weight, BigDecimal length, BigDecimal width, BigDecimal height,
                                    Boolean fragile, String notes, String photoUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("[PKG: ");
        if (weight != null) sb.append("weight=").append(weight.toPlainString()).append("kg, ");
        if (length != null && width != null && height != null) {
            sb.append("dims=").append(length.toPlainString()).append("x")
              .append(width.toPlainString()).append("x")
              .append(height.toPlainString()).append("cm, ");
        }
        sb.append("fragile=").append(Boolean.TRUE.equals(fragile)).append("]");

        if (photoUrl != null && !photoUrl.isBlank()) {
            sb.append(" [PKG_PHOTO: ").append(photoUrl).append("]");
        }
        if (notes != null && !notes.isBlank()) {
            sb.append(" ").append(notes);
        }
        String encoded = sb.toString().trim();
        return encoded.length() > 500 ? encoded.substring(0, 497) + "..." : encoded;
    }

    public DeliveryAddressInfo buildDeliveryAddress(OrderEntity order) {
        if (order == null) return null;
        BigDecimal lat = order.getShippingAddress() != null ? order.getShippingAddress().getLatitude() : null;
        BigDecimal lng = order.getShippingAddress() != null ? order.getShippingAddress().getLongitude() : null;

        return DeliveryAddressInfo.builder()
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
    }

    public DeliveryAddressInfo buildPickupAddress(SellerEntity seller, StoreEntity store) {
        String name = store != null ? store.getStoreName() : (seller != null ? seller.getBusinessName() : "Seller Hub");
        String phone = seller != null ? seller.getBusinessPhone() : null;

        return DeliveryAddressInfo.builder()
                .contactName(name)
                .phone(phone)
                .addressLine1("BazaarHub Fulfillment Merchant Store, Commercial Street")
                .addressLine2(store != null ? store.getDescription() : null)
                .landmark("Near Central Market")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .latitude(new BigDecimal("12.9716000"))
                .longitude(new BigDecimal("77.5946000"))
                .build();
    }
}
