package com.bazaarhub.backend.order.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class InvalidOrderOperationException extends BusinessRuleViolationException {

    public InvalidOrderOperationException(String message) {
        super(message, "INVALID_ORDER_OPERATION");
    }
}
