package com.bazaarhub.backend.delivery.domain.model;

/**
 * Operational availability status of a delivery partner.
 * Strictly maps to MariaDB enum('AVAILABLE','BUSY','OFFLINE','SUSPENDED').
 */
public enum DeliveryBoyAvailabilityStatus {
    AVAILABLE,
    BUSY,
    OFFLINE,
    SUSPENDED
}
