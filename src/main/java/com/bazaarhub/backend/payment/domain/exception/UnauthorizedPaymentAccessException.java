package com.bazaarhub.backend.payment.domain.exception;

public class UnauthorizedPaymentAccessException extends RuntimeException {

    public UnauthorizedPaymentAccessException(String message) {
        super(message);
    }
}
