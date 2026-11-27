package com.bazaarhub.backend.shipping.application.service;

import com.bazaarhub.backend.shipping.application.dto.response.PublicTrackingResponse;
import com.bazaarhub.backend.shipping.application.dto.response.TrackingHistoryResponse;

public interface ShipmentTrackingService {

    PublicTrackingResponse trackByTrackingNumber(String trackingNumber);

    TrackingHistoryResponse getTrackingHistory(Long userId, Long shipmentId, boolean isAdmin);
}
