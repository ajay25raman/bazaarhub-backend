package com.bazaarhub.backend.identity.domain.model;

/**
 * Account status matching the MariaDB `users.account_status` ENUM definition:
 * enum('ACTIVE','INACTIVE','BLOCKED','PENDING')
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED,
    PENDING
}
