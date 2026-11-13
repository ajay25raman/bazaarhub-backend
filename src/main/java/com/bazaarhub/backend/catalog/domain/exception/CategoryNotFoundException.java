package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(String message) {
        super(message);
    }

    public CategoryNotFoundException(Long categoryId) {
        super("Category", categoryId);
    }

    public CategoryNotFoundException(String field, String value) {
        super(String.format("Category with %s '%s' was not found", field, value));
    }
}
