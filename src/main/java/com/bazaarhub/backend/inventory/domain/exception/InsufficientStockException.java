package com.bazaarhub.backend.inventory.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class InsufficientStockException extends BusinessRuleViolationException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
