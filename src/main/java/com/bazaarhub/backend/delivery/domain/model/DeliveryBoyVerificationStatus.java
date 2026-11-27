package com.bazaarhub.backend.delivery.domain.model;

/**
 * Verification status of a delivery boy's KYC and profile.
 * Strictly maps to MariaDB enum('PENDING','VERIFIED','REJECTED').
 */
public enum DeliveryBoyVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
