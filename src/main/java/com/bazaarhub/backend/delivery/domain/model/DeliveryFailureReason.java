package com.bazaarhub.backend.delivery.domain.model;

/**
 * Standard business reasons for a failed delivery attempt.
 */
public enum DeliveryFailureReason {
    CUSTOMER_UNAVAILABLE,
    INCORRECT_ADDRESS,
    CUSTOMER_REJECTED,
    PAYMENT_ISSUE,
    DAMAGED_PARCEL,
    SECURITY_ACCESS_DENIED,
    WEATHER_OR_NATURAL_HAZARD,
    OTHER
}
