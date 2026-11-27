package com.bazaarhub.backend.payment.domain.model;

/**
 * Payment transaction status strictly matching MariaDB `payment_transactions.status` enum definition.
 */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}
