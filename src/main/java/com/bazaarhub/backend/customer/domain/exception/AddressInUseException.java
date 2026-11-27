package com.bazaarhub.backend.customer.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class AddressInUseException extends DomainException {
    public AddressInUseException(String message) {
        super(message, "ADDRESS_IN_USE");
    }
}
