package com.bazaarhub.backend.inventory.domain.model;

/**
 * Inventory stock movement types strictly matching MariaDB `inventory_movements` enum.
 */
public enum MovementType {
    STOCK_IN,
    STOCK_OUT,
    ADJUSTMENT,
    RESERVE,
    RELEASE,
    RETURN
}
