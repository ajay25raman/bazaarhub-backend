package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryAssignmentResponse;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryAssignmentService {

    DeliveryAssignmentResponse adminAssignDeliveryBoy(Long adminId, AssignDeliveryBoyRequest request);

    DeliveryAssignmentResponse adminReassignDeliveryBoy(Long adminId, Long assignmentId, ReassignDeliveryRequest request);

    Page<DeliveryAssignmentResponse> getDeliveryBoyAssignments(
            Long userId, DeliveryAssignmentStatus status, Pageable pageable);

    DeliveryAssignmentResponse getAssignmentById(Long userId, Long assignmentId, boolean isAdmin);

    DeliveryAssignmentResponse acceptAssignment(Long userId, Long assignmentId);

    DeliveryAssignmentResponse rejectAssignment(Long userId, Long assignmentId, RejectAssignmentRequest request);

    DeliveryAssignmentResponse confirmPickup(Long userId, Long assignmentId, PickupConfirmationRequest request);

    DeliveryAssignmentResponse markOutForDelivery(Long userId, Long assignmentId);

    DeliveryAssignmentResponse completeDelivery(Long userId, Long assignmentId, CompleteDeliveryRequest request);

    DeliveryAssignmentResponse failDelivery(Long userId, Long assignmentId, FailedDeliveryRequest request);

    Page<DeliveryAssignmentResponse> adminListAssignments(
            Long deliveryBoyId, Long shipmentId, DeliveryAssignmentStatus status, Pageable pageable);
}
