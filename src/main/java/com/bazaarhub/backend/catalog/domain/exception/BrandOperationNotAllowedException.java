package com.bazaarhub.backend.catalog.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class BrandOperationNotAllowedException extends BusinessRuleViolationException {

    public BrandOperationNotAllowedException(String message) {
        super(message, "BRAND_OPERATION_NOT_ALLOWED");
    }
}
