package com.bazaarhub.backend.payment.domain.model;

/**
 * Payment transaction type strictly matching MariaDB `payment_transactions.transaction_type` enum definition.
 */
public enum TransactionType {
    AUTHORIZATION,
    CAPTURE,
    PAYMENT,
    VOID,
    REFUND
}
