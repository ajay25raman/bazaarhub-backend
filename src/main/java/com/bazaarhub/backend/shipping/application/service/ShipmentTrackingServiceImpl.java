package com.bazaarhub.backend.shipping.application.service;

import com.bazaarhub.backend.delivery.application.dto.response.DeliveryTrackingResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.domain.exception.ShipmentNotFoundException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryTrackingRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.shipping.application.dto.response.PublicTrackingResponse;
import com.bazaarhub.backend.shipping.application.dto.response.TrackingHistoryResponse;
import com.bazaarhub.backend.shipping.application.mapper.ShippingMapper;
import com.bazaarhub.backend.shipping.domain.exception.UnauthorizedShipmentAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ShipmentTrackingServiceImpl implements ShipmentTrackingService {

    private final SpringDataShipmentRepository shipmentRepository;
    private final SpringDataDeliveryAssignmentRepository assignmentRepository;
    private final SpringDataDeliveryTrackingRepository trackingRepository;
    private final ShippingMapper shippingMapper;
    private final DeliveryMapper deliveryMapper;

    public ShipmentTrackingServiceImpl(SpringDataShipmentRepository shipmentRepository,
                                       SpringDataDeliveryAssignmentRepository assignmentRepository,
                                       SpringDataDeliveryTrackingRepository trackingRepository,
                                       ShippingMapper shippingMapper,
                                       DeliveryMapper deliveryMapper) {
        this.shipmentRepository = shipmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.trackingRepository = trackingRepository;
        this.shippingMapper = shippingMapper;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicTrackingResponse trackByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ShipmentNotFoundException("Tracking number cannot be empty");
        }

        ShipmentEntity shipment = shipmentRepository.findByTrackingNumber(trackingNumber.trim())
                .or(() -> shipmentRepository.findByShipmentNumber(trackingNumber.trim()))
                .orElseThrow(() -> new ShipmentNotFoundException("No shipment found for tracking number: " + trackingNumber));

        return shippingMapper.toPublicTrackingResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingHistoryResponse getTrackingHistory(Long userId, Long shipmentId, boolean isAdmin) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        SellerOrderEntity sellerOrder = shipment.getSellerOrder();
        OrderEntity order = sellerOrder != null ? sellerOrder.getOrder() : null;
        SellerEntity seller = sellerOrder != null ? sellerOrder.getSeller() : null;

        // Verify authorization if not admin
        if (!isAdmin && userId != null) {
            boolean isCustomer = order != null && order.getUser() != null && order.getUser().getId().equals(userId);
            boolean isSeller = seller != null && seller.getUser() != null && seller.getUser().getId().equals(userId);

            boolean isAssignedDeliveryBoy = false;
            Optional<DeliveryAssignmentEntity> activeAssignment = assignmentRepository
                    .findFirstByShipmentIdAndAssignmentStatusNotOrderByAssignedAtDesc(shipmentId, DeliveryAssignmentStatus.CANCELLED);
            if (activeAssignment.isPresent() && activeAssignment.get().getDeliveryBoy() != null
                    && activeAssignment.get().getDeliveryBoy().getUser() != null) {
                isAssignedDeliveryBoy = activeAssignment.get().getDeliveryBoy().getUser().getId().equals(userId);
            }

            if (!isCustomer && !isSeller && !isAssignedDeliveryBoy) {
                throw new UnauthorizedShipmentAccessException("You are not authorized to view tracking history for this shipment");
            }
        }

        // Active Delivery Boy Assignment Info
        Optional<DeliveryAssignmentEntity> assignmentOpt = assignmentRepository
                .findFirstByShipmentIdAndAssignmentStatusNotOrderByAssignedAtDesc(shipmentId, DeliveryAssignmentStatus.CANCELLED);

        String partnerName = null;
        String partnerPhone = null;
        String vehicle = null;

        if (assignmentOpt.isPresent()) {
            DeliveryAssignmentEntity assignment = assignmentOpt.get();
            if (assignment.getDeliveryBoy() != null) {
                UserEntity boyUser = assignment.getDeliveryBoy().getUser();
                partnerName = boyUser != null ? boyUser.getFullName() : "Delivery Partner";
                vehicle = assignment.getDeliveryBoy().getVehicleType() != null
                        ? assignment.getDeliveryBoy().getVehicleType().name() : null;

                String rawPhone = boyUser != null ? boyUser.getPhone() : null;
                if (rawPhone != null && rawPhone.length() >= 6) {
                    partnerPhone = rawPhone.substring(0, 3) + "*****" + rawPhone.substring(rawPhone.length() - 3);
                }
            }
        }

        // GPS Breadcrumbs
        List<DeliveryTrackingResponse> breadcrumbs = trackingRepository
                .findByShipmentIdOrderByRecordedAtDesc(shipmentId, PageRequest.of(0, 50))
                .map(deliveryMapper::toTrackingResponse)
                .getContent();

        String originCity = "Bengaluru";
        String destCity = order != null ? order.getCity() : "Destination Hub";

        return TrackingHistoryResponse.builder()
                .shipmentId(shipment.getId())
                .shipmentNumber(shipment.getShipmentNumber())
                .trackingNumber(shipment.getTrackingNumber())
                .carrierName(shipment.getCarrierName() != null ? shipment.getCarrierName() : "BazaarHub Express")
                .status(shipment.getStatus())
                .pickupAddress(shippingMapper.buildPickupAddress(seller, seller != null ? seller.getStore() : null))
                .deliveryAddress(shippingMapper.buildDeliveryAddress(order))
                .deliveryPartnerName(partnerName)
                .deliveryPartnerPhone(partnerPhone)
                .vehicleType(vehicle)
                .shippedAt(shipment.getShippedAt())
                .estimatedDeliveryAt(shipment.getEstimatedDeliveryAt())
                .deliveredAt(shipment.getDeliveredAt())
                .deliveryNotes(shipment.getDeliveryNotes())
                .milestones(shippingMapper.buildMilestones(shipment, originCity, destCity))
                .breadcrumbs(breadcrumbs)
                .build();
    }
}
