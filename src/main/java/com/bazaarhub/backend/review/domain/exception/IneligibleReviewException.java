package com.bazaarhub.backend.review.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

/**
 * Thrown when a customer attempts to review a product they have not purchased or whose order has not been delivered.
 */
public class IneligibleReviewException extends BusinessRuleViolationException {

    public IneligibleReviewException(String message) {
        super(message, "INELIGIBLE_REVIEW");
    }

    public IneligibleReviewException(Long customerId, Long productId) {
        super(String.format("Customer %d is ineligible to review product %d. Only verified purchasers with delivered orders may submit reviews.",
                customerId, productId), "INELIGIBLE_REVIEW");
    }
}
