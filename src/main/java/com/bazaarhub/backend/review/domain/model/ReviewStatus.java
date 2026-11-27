package com.bazaarhub.backend.review.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Review moderation status strictly matching MariaDB `product_reviews.status` enum:
 * enum('pending','approved','rejected').
 */
public enum ReviewStatus {
    pending,
    approved,
    rejected;

    @JsonValue
    public String toValue() {
        return this.name();
    }

    @JsonCreator
    public static ReviewStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ReviewStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReviewStatus: " + value + ". Permitted values: [pending, approved, rejected]");
    }
}
