package com.bazaarhub.backend.customer.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedAddressAccessException extends DomainException {
    public UnauthorizedAddressAccessException(String message) {
        super(message, "UNAUTHORIZED_ADDRESS_ACCESS");
    }
}
