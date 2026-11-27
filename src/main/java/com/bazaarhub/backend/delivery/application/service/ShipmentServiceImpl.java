package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.CreateShipmentRequest;
import com.bazaarhub.backend.delivery.application.dto.response.ShipmentResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.domain.exception.ShipmentNotFoundException;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.domain.exception.SellerOrderNotFoundException;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);

    private final SpringDataShipmentRepository shipmentRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataUserRepository userRepository;
    private final DeliveryMapper deliveryMapper;

    public ShipmentServiceImpl(SpringDataShipmentRepository shipmentRepository,
                               SpringDataSellerOrderRepository sellerOrderRepository,
                               SpringDataUserRepository userRepository,
                               DeliveryMapper deliveryMapper) {
        this.shipmentRepository = shipmentRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.userRepository = userRepository;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    @Transactional
    public ShipmentResponse createShipment(Long creatorId, CreateShipmentRequest request) {
        SellerOrderEntity sellerOrder = sellerOrderRepository.findById(request.getSellerOrderId())
                .orElseThrow(() -> new SellerOrderNotFoundException("Seller order not found with ID: " + request.getSellerOrderId()));

        UserEntity creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + creatorId));

        String shipmentNumber = "SHP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        ShipmentEntity entity = ShipmentEntity.builder()
                .sellerOrder(sellerOrder)
                .shipmentNumber(shipmentNumber)
                .carrierName(request.getCarrierName() != null ? request.getCarrierName() : "BazaarHub Express")
                .trackingNumber(request.getTrackingNumber() != null ? request.getTrackingNumber() : shipmentNumber)
                .status(ShipmentStatus.READY_FOR_PICKUP)
                .estimatedDeliveryAt(request.getEstimatedDeliveryAt() != null ? request.getEstimatedDeliveryAt() : Instant.now().plusSeconds(86400 * 2))
                .deliveryNotes(request.getDeliveryNotes())
                .createdBy(creator)
                .updatedBy(creator)
                .build();

        ShipmentEntity saved = shipmentRepository.save(entity);
        log.info("[SHIPMENT CREATED] Created shipment {} for seller order: {}", saved.getShipmentNumber(), sellerOrder.getSubOrderNumber());

        return deliveryMapper.toShipmentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long shipmentId) {
        ShipmentEntity entity = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        return deliveryMapper.toShipmentResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByNumber(String shipmentNumber) {
        ShipmentEntity entity = shipmentRepository.findByShipmentNumber(shipmentNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with number: " + shipmentNumber));
        return deliveryMapper.toShipmentResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> listShipments(ShipmentStatus status, Long sellerOrderId, Pageable pageable) {
        return shipmentRepository.searchShipments(status, sellerOrderId, pageable)
                .map(deliveryMapper::toShipmentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getCustomerShipments(Long customerId, Pageable pageable) {
        return shipmentRepository.findByCustomerId(customerId, pageable)
                .map(deliveryMapper::toShipmentResponse);
    }
}
