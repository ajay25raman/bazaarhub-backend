package com.bazaarhub.backend.delivery.domain.model;

/**
 * Supported vehicle types for delivery partners.
 * Strictly maps to MariaDB enum('BIKE','SCOOTER','BICYCLE','OTHER').
 */
public enum VehicleType {
    BIKE,
    SCOOTER,
    BICYCLE,
    OTHER
}
