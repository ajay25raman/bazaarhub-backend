package com.bazaarhub.backend.customer.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class CustomerProfileNotFoundException extends ResourceNotFoundException {
    public CustomerProfileNotFoundException(String message) {
        super(message);
    }

    public CustomerProfileNotFoundException(Long userId) {
        super(String.format("Customer profile for user ID %d was not found", userId));
    }
}
