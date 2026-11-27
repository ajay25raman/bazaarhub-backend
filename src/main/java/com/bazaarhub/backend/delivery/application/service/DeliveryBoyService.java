package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryBoyResponse;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryBoyService {

    DeliveryBoyResponse onboard(Long userId, DeliveryBoyOnboardRequest request);

    DeliveryBoyResponse getProfile(Long userId);

    DeliveryBoyResponse getDeliveryBoyById(Long deliveryBoyId);

    DeliveryBoyResponse updateProfile(Long userId, DeliveryBoyUpdateRequest request);

    DeliveryBoyResponse updateAvailability(Long userId, UpdateAvailabilityRequest request);

    DeliveryBoyResponse adminVerifyDeliveryBoy(Long adminId, Long deliveryBoyId, AdminVerifyDeliveryBoyRequest request);

    DeliveryBoyResponse adminUpdateStatus(Long adminId, Long deliveryBoyId, AdminUpdateDeliveryBoyStatusRequest request);

    Page<DeliveryBoyResponse> adminListDeliveryBoys(
            DeliveryBoyVerificationStatus verificationStatus,
            DeliveryBoyAvailabilityStatus availabilityStatus,
            Pageable pageable);
}
