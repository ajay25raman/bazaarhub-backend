package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidProductStateException extends DomainException {

    public InvalidProductStateException(String message) {
        super(message, "INVALID_PRODUCT_STATE");
    }
}
