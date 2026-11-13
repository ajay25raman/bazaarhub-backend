package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class SkuAlreadyExistsException extends DomainException {

    public SkuAlreadyExistsException(String message) {
        super(message, "SKU_ALREADY_EXISTS");
    }
}
