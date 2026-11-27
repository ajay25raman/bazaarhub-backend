package com.bazaarhub.backend.shipping.domain.model;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Value object summarizing non-sensitive, public tracking data.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTrackingSummary {

    private String trackingNumber;

    private String shipmentNumber;

    private String carrierName;

    private ShipmentStatus status;

    private String originCity;

    private String destinationCity;

    private String destinationState;

    private Instant shippedAt;

    private Instant estimatedDeliveryAt;

    private Instant deliveredAt;

    private List<TrackingMilestone> milestones;
}
