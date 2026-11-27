package com.bazaarhub.backend.shipping.application.service;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.shipping.application.dto.request.CancelShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.CreateInternalShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ReadyForPickupRequest;
import com.bazaarhub.backend.shipping.application.dto.response.InternalShipmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InternalShippingService {

    InternalShipmentResponse createShipment(Long userId, CreateInternalShipmentRequest request, boolean isAdmin);

    InternalShipmentResponse getShipmentById(Long userId, Long shipmentId, boolean isAdmin);

    List<InternalShipmentResponse> getShipmentsByOrderId(Long userId, Long orderId, boolean isAdmin);

    Page<InternalShipmentResponse> getSellerShipments(Long userId, ShipmentStatus status, Pageable pageable);

    InternalShipmentResponse getSellerShipmentById(Long userId, Long shipmentId);

    InternalShipmentResponse markReadyForPickup(Long userId, Long shipmentId, ReadyForPickupRequest request);

    InternalShipmentResponse cancelShipment(Long userId, Long shipmentId, CancelShipmentRequest request, boolean isAdmin);

    Page<InternalShipmentResponse> getCustomerShipments(Long customerId, Pageable pageable);

    InternalShipmentResponse getCustomerShipmentById(Long customerId, Long shipmentId);

    Page<InternalShipmentResponse> adminListShipments(String search, ShipmentStatus status, Long sellerId, Long customerId, Pageable pageable);

    Page<InternalShipmentResponse> adminListActiveShipments(Pageable pageable);

    Page<InternalShipmentResponse> adminListFailedShipments(Pageable pageable);
}
