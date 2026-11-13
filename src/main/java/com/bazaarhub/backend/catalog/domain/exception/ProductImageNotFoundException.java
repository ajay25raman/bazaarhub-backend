package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class ProductImageNotFoundException extends ResourceNotFoundException {

    public ProductImageNotFoundException(String message) {
        super(message);
    }

    public ProductImageNotFoundException(Long imageId) {
        super("ProductImage", imageId);
    }
}
