package com.bazaarhub.backend.shared.domain.exception;

/**
 * Thrown when an operation violates invariant business rules.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleViolationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
