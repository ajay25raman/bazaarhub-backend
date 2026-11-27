package com.bazaarhub.backend.review.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

/**
 * Thrown when an invalid operation is performed on a review (e.g. invalid rating range, invalid transition).
 */
public class InvalidReviewOperationException extends BusinessRuleViolationException {

    public InvalidReviewOperationException(String message) {
        super(message, "INVALID_REVIEW_OPERATION");
    }
}
