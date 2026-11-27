package com.bazaarhub.backend.order.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class InvalidOrderStatusTransitionException extends BusinessRuleViolationException {

    public InvalidOrderStatusTransitionException(String message) {
        super(message, "INVALID_ORDER_STATUS_TRANSITION");
    }
}
