package com.bazaarhub.backend.cart.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidCartOperationException extends DomainException {

    public InvalidCartOperationException(String message) {
        super(message, "INVALID_CART_OPERATION");
    }
}
