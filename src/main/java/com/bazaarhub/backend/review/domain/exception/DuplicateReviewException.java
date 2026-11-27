package com.bazaarhub.backend.review.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

/**
 * Thrown when a customer attempts to submit more than one review for the same product.
 */
public class DuplicateReviewException extends BusinessRuleViolationException {

    public DuplicateReviewException(String message) {
        super(message, "DUPLICATE_REVIEW");
    }

    public DuplicateReviewException(Long customerId, Long productId) {
        super(String.format("Customer %d has already submitted a review for product %d. Please update your existing review.",
                customerId, productId), "DUPLICATE_REVIEW");
    }
}
