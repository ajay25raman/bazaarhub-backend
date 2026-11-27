package com.bazaarhub.backend.inventory.domain.model;

/**
 * Stock reservation lifecycle states strictly matching MariaDB `stock_reservations` enum.
 */
public enum ReservationStatus {
    ACTIVE,
    CONFIRMED,
    RELEASED,
    EXPIRED,
    CANCELLED
}
