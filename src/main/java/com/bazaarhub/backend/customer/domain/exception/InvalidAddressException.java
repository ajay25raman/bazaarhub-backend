package com.bazaarhub.backend.customer.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidAddressException extends DomainException {
    public InvalidAddressException(String message) {
        super(message, "INVALID_ADDRESS");
    }
}
