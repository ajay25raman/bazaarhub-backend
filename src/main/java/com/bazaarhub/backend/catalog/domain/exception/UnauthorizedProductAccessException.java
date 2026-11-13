package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedProductAccessException extends DomainException {

    public UnauthorizedProductAccessException(String message) {
        super(message, "UNAUTHORIZED_PRODUCT_ACCESS");
    }
}
