package com.bazaarhub.backend.order.domain.model;

/**
 * Seller sub-order status strictly matching MariaDB `seller_orders.status` enum.
 */
public enum SellerOrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}
