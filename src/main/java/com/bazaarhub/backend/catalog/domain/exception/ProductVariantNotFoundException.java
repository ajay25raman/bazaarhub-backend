package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class ProductVariantNotFoundException extends ResourceNotFoundException {

    public ProductVariantNotFoundException(String message) {
        super(message);
    }

    public ProductVariantNotFoundException(Long variantId) {
        super("ProductVariant", variantId);
    }

    public ProductVariantNotFoundException(String field, String value) {
        super(String.format("ProductVariant with %s '%s' was not found", field, value));
    }
}
