package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidCategoryHierarchyException extends DomainException {

    public InvalidCategoryHierarchyException(String message) {
        super(message, "INVALID_CATEGORY_HIERARCHY");
    }
}
