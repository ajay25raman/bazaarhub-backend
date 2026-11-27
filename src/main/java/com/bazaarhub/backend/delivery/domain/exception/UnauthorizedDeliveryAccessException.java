package com.bazaarhub.backend.delivery.domain.exception;

public class UnauthorizedDeliveryAccessException extends RuntimeException {

    public UnauthorizedDeliveryAccessException(String message) {
        super(message);
    }
}
