package com.bazaarhub.backend.search.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

/**
 * Thrown when search or filter criteria are invalid (e.g., negative prices, minPrice > maxPrice, page size exceeds limit).
 */
public class InvalidSearchFilterException extends DomainException {

    public InvalidSearchFilterException(String message) {
        super(message, "INVALID_SEARCH_FILTER");
    }
}
