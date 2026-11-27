package com.bazaarhub.backend.cart.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

import java.util.Collections;
import java.util.List;

public class CheckoutValidationException extends BusinessRuleViolationException {

    private final List<String> validationErrors;

    public CheckoutValidationException(String message, List<String> validationErrors) {
        super(message, "CHECKOUT_VALIDATION_FAILED");
        this.validationErrors = validationErrors != null ? validationErrors : Collections.emptyList();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
