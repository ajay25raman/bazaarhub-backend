package com.bazaarhub.backend.cart.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedCartAccessException extends DomainException {

    public UnauthorizedCartAccessException(String message) {
        super(message, "UNAUTHORIZED_CART_ACCESS");
    }
}
