package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(String message) {
        super(message);
    }

    public ProductNotFoundException(Long productId) {
        super("Product", productId);
    }

    public ProductNotFoundException(String field, String value) {
        super(String.format("Product with %s '%s' was not found", field, value));
    }
}
