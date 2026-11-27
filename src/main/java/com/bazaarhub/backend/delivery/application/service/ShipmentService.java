package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.CreateShipmentRequest;
import com.bazaarhub.backend.delivery.application.dto.response.ShipmentResponse;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShipmentService {

    ShipmentResponse createShipment(Long creatorId, CreateShipmentRequest request);

    ShipmentResponse getShipmentById(Long shipmentId);

    ShipmentResponse getShipmentByNumber(String shipmentNumber);

    Page<ShipmentResponse> listShipments(ShipmentStatus status, Long sellerOrderId, Pageable pageable);

    Page<ShipmentResponse> getCustomerShipments(Long customerId, Pageable pageable);
}
