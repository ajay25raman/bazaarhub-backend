package com.bazaarhub.backend.delivery.domain.model;

/**
 * Status of a delivery assignment task.
 * Strictly maps to MariaDB enum('ASSIGNED','ACCEPTED','REJECTED','PICKED_UP','COMPLETED','CANCELLED').
 */
public enum DeliveryAssignmentStatus {
    ASSIGNED,
    ACCEPTED,
    REJECTED,
    PICKED_UP,
    COMPLETED,
    CANCELLED
}
