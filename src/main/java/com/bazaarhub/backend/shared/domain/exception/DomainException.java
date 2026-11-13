package com.bazaarhub.backend.shared.domain.exception;

/**
 * Base abstract exception for all domain-specific errors in the BazaarHub platform.
 * Domain exceptions are independent of any framework or transport protocol.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
