package com.bazaarhub.backend.order.domain.model;

/**
 * Order lifecycle status strictly matching MariaDB `orders.order_status` enum.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}
