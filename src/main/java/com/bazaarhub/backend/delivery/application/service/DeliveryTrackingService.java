package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.UpdateLocationRequest;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryTrackingResponse;
import com.bazaarhub.backend.delivery.application.dto.response.RouteEstimateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryTrackingService {

    DeliveryTrackingResponse recordLocation(Long userId, UpdateLocationRequest request);

    DeliveryTrackingResponse getLatestLocation(Long userId, Long shipmentId, boolean isAdmin);

    default DeliveryTrackingResponse getLatestLocation(Long shipmentId) {
        return getLatestLocation(null, shipmentId, true);
    }

    Page<DeliveryTrackingResponse> getLocationHistory(Long shipmentId, Pageable pageable);

    RouteEstimateResponse getRouteEstimate(Long userId, Long shipmentId, boolean isAdmin);

    default RouteEstimateResponse getRouteEstimate(Long shipmentId) {
        return getRouteEstimate(null, shipmentId, true);
    }
}
