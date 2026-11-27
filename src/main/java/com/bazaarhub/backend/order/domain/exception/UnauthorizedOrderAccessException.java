package com.bazaarhub.backend.order.domain.exception;

public class UnauthorizedOrderAccessException extends RuntimeException {

    public UnauthorizedOrderAccessException(String message) {
        super(message);
    }
}
