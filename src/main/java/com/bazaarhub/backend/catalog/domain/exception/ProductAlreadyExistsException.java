package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class ProductAlreadyExistsException extends DomainException {

    public ProductAlreadyExistsException(String message) {
        super(message, "PRODUCT_ALREADY_EXISTS");
    }
}
