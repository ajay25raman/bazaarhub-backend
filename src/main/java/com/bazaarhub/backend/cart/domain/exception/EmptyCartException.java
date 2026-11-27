package com.bazaarhub.backend.cart.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class EmptyCartException extends BusinessRuleViolationException {

    public EmptyCartException(String message) {
        super(message, "EMPTY_CART");
    }
}
