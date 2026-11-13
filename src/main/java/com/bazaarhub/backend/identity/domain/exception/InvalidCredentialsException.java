package com.bazaarhub.backend.identity.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

/**
 * Thrown when user credentials (email/phone or password) are invalid.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid email/phone or password", "INVALID_CREDENTIALS");
    }

    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS");
    }
}
