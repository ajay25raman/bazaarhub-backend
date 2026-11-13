package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class CircularCategoryHierarchyException extends DomainException {

    public CircularCategoryHierarchyException(String message) {
        super(message, "CIRCULAR_CATEGORY_HIERARCHY");
    }
}
