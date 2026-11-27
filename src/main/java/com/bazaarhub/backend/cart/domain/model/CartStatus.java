package com.bazaarhub.backend.cart.domain.model;

/**
 * Status lifecycle of a customer shopping cart mapping directly to MariaDB carts.status enum.
 */
public enum CartStatus {
    ACTIVE,
    CONVERTED,
    ABANDONED
}
