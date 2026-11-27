package com.bazaarhub.backend.payment.domain.exception;

public class InvalidRefundOperationException extends RuntimeException {

    public InvalidRefundOperationException(String message) {
        super(message);
    }
}
