package com.bazaarhub.backend.support.domain.model;

/**
 * Standard business category for customer, seller, or delivery support tickets.
 * Mapped to VARCHAR(50) in support_tickets table.
 */
public enum TicketCategory {
    ORDER_ISSUE,
    PAYMENT_ISSUE,
    REFUND_ISSUE,
    DELIVERY_ISSUE,
    PRODUCT_ISSUE,
    SELLER_ISSUE,
    ACCOUNT_ISSUE,
    OTHER
}
