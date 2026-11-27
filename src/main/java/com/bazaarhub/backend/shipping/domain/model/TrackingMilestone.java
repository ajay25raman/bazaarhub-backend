package com.bazaarhub.backend.shipping.domain.model;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Value object representing an event along the shipment lifecycle timeline.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingMilestone {

    private ShipmentStatus status;

    private String title;

    private String description;

    private Instant timestamp;

    private String location;

    private boolean completed;
}
