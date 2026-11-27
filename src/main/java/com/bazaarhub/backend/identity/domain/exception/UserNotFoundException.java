package com.bazaarhub.backend.identity.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super(String.format("User with ID %d was not found", userId));
    }
}
