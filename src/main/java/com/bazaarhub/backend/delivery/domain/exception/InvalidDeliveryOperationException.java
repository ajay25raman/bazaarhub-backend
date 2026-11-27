package com.bazaarhub.backend.delivery.domain.exception;

public class InvalidDeliveryOperationException extends RuntimeException {

    public InvalidDeliveryOperationException(String message) {
        super(message);
    }
}
