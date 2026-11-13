package com.bazaarhub.backend.seller.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;

public class InvalidSellerStateException extends BusinessRuleViolationException {

    public InvalidSellerStateException(String message) {
        super(message);
    }
}
