package com.bazaarhub.backend.delivery.domain.model;

/**
 * Lifecycle status of a shipment.
 * Strictly maps to MariaDB enum('PENDING','READY_FOR_PICKUP','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','FAILED','RETURNED','CANCELLED').
 */
public enum ShipmentStatus {
    PENDING,
    READY_FOR_PICKUP,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    RETURNED,
    CANCELLED
}
