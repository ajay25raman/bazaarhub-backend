package com.bazaarhub.backend.inventory.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class InvalidReservationStateException extends BusinessRuleViolationException {

    public InvalidReservationStateException(String message) {
        super(message);
    }
}
