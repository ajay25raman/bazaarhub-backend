package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.UpdateLocationRequest;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryTrackingResponse;
import com.bazaarhub.backend.delivery.application.dto.response.RouteEstimateResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.domain.exception.DeliveryBoyNotFoundException;
import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOperationException;
import com.bazaarhub.backend.delivery.domain.exception.ShipmentNotFoundException;
import com.bazaarhub.backend.delivery.domain.exception.UnauthorizedDeliveryAccessException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.adapter.GoogleMapsService;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryTrackingEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryBoyRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryTrackingRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryTrackingServiceImpl implements DeliveryTrackingService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryTrackingServiceImpl.class);

    private final SpringDataDeliveryTrackingRepository trackingRepository;
    private final SpringDataDeliveryBoyRepository deliveryBoyRepository;
    private final SpringDataShipmentRepository shipmentRepository;
    private final SpringDataDeliveryAssignmentRepository assignmentRepository;
    private final GoogleMapsService googleMapsService;
    private final DeliveryMapper deliveryMapper;

    public DeliveryTrackingServiceImpl(SpringDataDeliveryTrackingRepository trackingRepository,
                                       SpringDataDeliveryBoyRepository deliveryBoyRepository,
                                       SpringDataShipmentRepository shipmentRepository,
                                       SpringDataDeliveryAssignmentRepository assignmentRepository,
                                       GoogleMapsService googleMapsService,
                                       DeliveryMapper deliveryMapper) {
        this.trackingRepository = trackingRepository;
        this.deliveryBoyRepository = deliveryBoyRepository;
        this.shipmentRepository = shipmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.googleMapsService = googleMapsService;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    @Transactional
    public DeliveryTrackingResponse recordLocation(Long userId, UpdateLocationRequest request) {
        DeliveryBoyEntity deliveryBoy = deliveryBoyRepository.findByUserId(userId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException("Delivery partner profile not found for user ID: " + userId));

        ShipmentEntity shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new ShipmentNotFoundException(request.getShipmentId()));

        if (shipment.getStatus() != ShipmentStatus.PICKED_UP &&
            shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY &&
            shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new InvalidDeliveryOperationException("Cannot log location. Shipment is not in an active transit state. Status: " + shipment.getStatus());
        }

        DeliveryTrackingEntity entity = DeliveryTrackingEntity.builder()
                .shipment(shipment)
                .deliveryBoy(deliveryBoy)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .recordedAt(Instant.now())
                .createdBy(deliveryBoy.getUser())
                .updatedBy(deliveryBoy.getUser())
                .build();

        DeliveryTrackingEntity saved = trackingRepository.save(entity);
        log.debug("[TRACKING RECORDED] Shipment: {}, DeliveryBoy: {}, Lat: {}, Lng: {}",
                shipment.getId(), deliveryBoy.getId(), request.getLatitude(), request.getLongitude());

        return deliveryMapper.toTrackingResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryTrackingResponse getLatestLocation(Long userId, Long shipmentId, boolean isAdmin) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        validateShipmentAccess(userId, shipment, isAdmin);

        Optional<DeliveryTrackingEntity> latest = trackingRepository.findFirstByShipmentIdOrderByRecordedAtDesc(shipmentId);
        return latest.map(deliveryMapper::toTrackingResponse).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryTrackingResponse> getLocationHistory(Long shipmentId, Pageable pageable) {
        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ShipmentNotFoundException(shipmentId);
        }

        return trackingRepository.findByShipmentIdOrderByRecordedAtDesc(shipmentId, pageable)
                .map(deliveryMapper::toTrackingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteEstimateResponse getRouteEstimate(Long userId, Long shipmentId, boolean isAdmin) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        validateShipmentAccess(userId, shipment, isAdmin);

        BigDecimal originLat = null;
        BigDecimal originLng = null;

        // Try getting latest live location of delivery partner
        Optional<DeliveryTrackingEntity> latestTracking = trackingRepository.findFirstByShipmentIdOrderByRecordedAtDesc(shipmentId);
        if (latestTracking.isPresent()) {
            originLat = latestTracking.get().getLatitude();
            originLng = latestTracking.get().getLongitude();
        }

        BigDecimal destLat = null;
        BigDecimal destLng = null;
        String destAddress = null;

        OrderEntity order = shipment.getSellerOrder() != null ? shipment.getSellerOrder().getOrder() : null;
        if (order != null) {
            if (order.getShippingAddress() != null) {
                destLat = order.getShippingAddress().getLatitude();
                destLng = order.getShippingAddress().getLongitude();
            }
            destAddress = order.getAddressLine1() + ", " + order.getCity() + ", " + order.getPostalCode();
        }

        GoogleMapsService.RouteDetails details = googleMapsService.calculateRoute(
                originLat, originLng, destLat, destLng, "Seller Hub", destAddress);

        return RouteEstimateResponse.builder()
                .distanceKm(details.distanceKm())
                .durationMinutes(details.durationMinutes())
                .summary(details.summary())
                .build();
    }

    /**
     * Enforces Broken Object Level Authorization (BOLA / IDOR) defense:
     * 1. Admins (ROLE_ADMIN) have platform-wide tracking oversight.
     * 2. Customers can only track shipments associated with their own placed orders.
     * 3. Delivery partners can only track shipments currently assigned to them.
     * All unauthorized access attempts are rejected with 403 Forbidden.
     */
    private void validateShipmentAccess(Long userId, ShipmentEntity shipment, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (userId == null) {
            throw new UnauthorizedDeliveryAccessException("Authentication is required to access shipment tracking");
        }

        // 1. Verify if requesting customer owns the order for this shipment
        if (shipment.getSellerOrder() != null &&
            shipment.getSellerOrder().getOrder() != null &&
            shipment.getSellerOrder().getOrder().getUser() != null &&
            shipment.getSellerOrder().getOrder().getUser().getId().equals(userId)) {
            return;
        }

        // 2. Verify if requesting delivery partner is assigned to this shipment
        boolean isAssigned = assignmentRepository.isDeliveryBoyAssignedToShipment(
                shipment.getId(), userId, List.of(DeliveryAssignmentStatus.REJECTED, DeliveryAssignmentStatus.CANCELLED));
        if (isAssigned) {
            return;
        }

        log.warn("[UNAUTHORIZED DELIVERY ACCESS] User {} attempted to access tracking for shipment {} without valid ownership or assignment",
                userId, shipment.getId());
        throw new UnauthorizedDeliveryAccessException("You are not authorized to access tracking for shipment ID: " + shipment.getId());
    }
}
