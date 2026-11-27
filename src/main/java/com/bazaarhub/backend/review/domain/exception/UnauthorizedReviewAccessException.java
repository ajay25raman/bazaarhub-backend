package com.bazaarhub.backend.review.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

/**
 * Thrown when an unauthorized user attempts to view, modify, or delete a review.
 */
public class UnauthorizedReviewAccessException extends DomainException {

    public UnauthorizedReviewAccessException(String message) {
        super(message, "UNAUTHORIZED_REVIEW_ACCESS");
    }

    public UnauthorizedReviewAccessException(Long userId, Long reviewId) {
        super(String.format("User %d is not authorized to access or modify review %d", userId, reviewId),
                "UNAUTHORIZED_REVIEW_ACCESS");
    }
}
