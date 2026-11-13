package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class CategoryAlreadyExistsException extends DomainException {

    public CategoryAlreadyExistsException(String message) {
        super(message, "CATEGORY_ALREADY_EXISTS");
    }
}
