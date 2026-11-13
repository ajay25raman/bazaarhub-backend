package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class CategoryOperationNotAllowedException extends BusinessRuleViolationException {

    public CategoryOperationNotAllowedException(String message) {
        super(message, "CATEGORY_OPERATION_NOT_ALLOWED");
    }
}
