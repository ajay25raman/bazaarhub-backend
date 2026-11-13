package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class BrandAlreadyExistsException extends DomainException {

    public BrandAlreadyExistsException(String message) {
        super(message, "BRAND_ALREADY_EXISTS");
    }
}
