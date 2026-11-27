package com.bazaarhub.backend.customer.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class AddressNotFoundException extends ResourceNotFoundException {
    public AddressNotFoundException(String message) {
        super(message);
    }

    public AddressNotFoundException(Long addressId) {
        super(String.format("Address with identifier '%d' was not found", addressId));
    }
}
