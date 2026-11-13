package com.bazaarhub.backend.identity.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

/**
 * Thrown when an account registration conflicts with an existing email or phone number.
 */
public class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException(String message) {
        super(message, "USER_ALREADY_EXISTS");
    }
}
