package com.bazaarhub.backend.inventory.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class InvalidStockOperationException extends BusinessRuleViolationException {

    public InvalidStockOperationException(String message) {
        super(message);
    }
}
