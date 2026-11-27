package com.bazaarhub.backend.inventory.domain.exception;

public class UnauthorizedInventoryAccessException extends RuntimeException {

    public UnauthorizedInventoryAccessException(String message) {
        super(message);
    }
}
