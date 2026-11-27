package com.bazaarhub.backend.review.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

/**
 * Thrown when a requested product review cannot be located.
 */
public class ReviewNotFoundException extends ResourceNotFoundException {

    public ReviewNotFoundException(String message) {
        super(message);
    }

    public ReviewNotFoundException(Long reviewId) {
        super("ProductReview", reviewId);
    }
}
