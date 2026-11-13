package com.bazaarhub.backend.identity.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

/**
 * Thrown when an account is not in ACTIVE state (e.g., BLOCKED, INACTIVE, or PENDING).
 */
public class AccountNotActiveException extends DomainException {

    public AccountNotActiveException(String message) {
        super(message, "ACCOUNT_NOT_ACTIVE");
    }
}
