package com.bazaarhub.backend.shipping.infrastructure.provider;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.shipping.application.dto.request.CreateInternalShipmentRequest;
import com.bazaarhub.backend.shipping.application.mapper.ShippingMapper;
import com.bazaarhub.backend.shipping.domain.exception.InvalidShipmentOperationException;
import com.bazaarhub.backend.shipping.domain.model.ShippingProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class InternalShippingProvider implements ShippingProvider {

    private static final Logger log = LoggerFactory.getLogger(InternalShippingProvider.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SpringDataShipmentRepository shipmentRepository;
    private final ShippingMapper shippingMapper;

    public InternalShippingProvider(SpringDataShipmentRepository shipmentRepository,
                                    ShippingMapper shippingMapper) {
        this.shipmentRepository = shipmentRepository;
        this.shippingMapper = shippingMapper;
    }

    @Override
    public ShippingProviderType getProviderType() {
        return ShippingProviderType.INTERNAL;
    }

    @Override
    public ShipmentEntity createShipment(SellerOrderEntity sellerOrder, CreateInternalShipmentRequest request, UserEntity actor) {
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String datePrefix = LocalDate.now().format(DATE_FORMAT);

        String shipmentNumber = "SHP-" + datePrefix + "-" + randomSuffix.substring(0, 6);
        String trackingNumber = "BZH-" + datePrefix + "-" + randomSuffix;

        int days = (request != null && request.getEstimatedDeliveryDays() != null && request.getEstimatedDeliveryDays() > 0)
                ? request.getEstimatedDeliveryDays()
                : 2;
        Instant estimatedDeliveryAt = Instant.now().plusSeconds(86400L * days);

        String encodedNotes = (request != null)
                ? shippingMapper.encodePackageInfo(
                        request.getPackageWeight(),
                        request.getLengthCm(),
                        request.getWidthCm(),
                        request.getHeightCm(),
                        request.getIsFragile(),
                        request.getDeliveryNotes(),
                        null)
                : null;

        String carrier = (request != null && request.getCarrierName() != null && !request.getCarrierName().isBlank())
                ? request.getCarrierName()
                : "BazaarHub Express";

        ShipmentEntity shipment = ShipmentEntity.builder()
                .sellerOrder(sellerOrder)
                .shipmentNumber(shipmentNumber)
                .trackingNumber(trackingNumber)
                .carrierName(carrier)
                .status(ShipmentStatus.PENDING)
                .estimatedDeliveryAt(estimatedDeliveryAt)
                .deliveryNotes(encodedNotes)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        ShipmentEntity saved = shipmentRepository.save(shipment);
        log.info("[INTERNAL SHIPMENT DISPATCHED] Shipment {} generated with tracking: {} for seller order: {}",
                saved.getShipmentNumber(), saved.getTrackingNumber(), sellerOrder.getSubOrderNumber());

        return saved;
    }

    @Override
    public ShipmentEntity cancelShipment(ShipmentEntity shipment, String reason, UserEntity actor) {
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentOperationException("Cannot cancel an already delivered shipment");
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        String currentNotes = shipment.getDeliveryNotes() != null ? shipment.getDeliveryNotes() : "";
        String cancelNote = "[CANCELLED: " + reason + "]";
        String combined = (currentNotes + " " + cancelNote).trim();
        shipment.setDeliveryNotes(combined.length() > 500 ? combined.substring(0, 497) + "..." : combined);
        shipment.setUpdatedBy(actor);
        shipment.setUpdatedAt(Instant.now());

        ShipmentEntity saved = shipmentRepository.save(shipment);
        log.info("[INTERNAL SHIPMENT CANCELLED] Shipment {} marked CANCELLED. Reason: {}",
                saved.getShipmentNumber(), reason);

        return saved;
    }
}
