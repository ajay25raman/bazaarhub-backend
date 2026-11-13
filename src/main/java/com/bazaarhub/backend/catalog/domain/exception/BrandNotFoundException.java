package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class BrandNotFoundException extends ResourceNotFoundException {

    public BrandNotFoundException(String message) {
        super(message);
    }

    public BrandNotFoundException(Long brandId) {
        super("Brand", brandId);
    }

    public BrandNotFoundException(String field, String value) {
        super(String.format("Brand with %s '%s' was not found", field, value));
    }
}
