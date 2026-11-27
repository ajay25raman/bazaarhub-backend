package com.bazaarhub.backend.customer.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class CustomerProfileAlreadyExistsException extends DomainException {
    public CustomerProfileAlreadyExistsException(String message) {
        super(message, "CUSTOMER_PROFILE_ALREADY_EXISTS");
    }
}
