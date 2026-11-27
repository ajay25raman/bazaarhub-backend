package com.bazaarhub.backend.customer.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedCustomerAccessException extends DomainException {
    public UnauthorizedCustomerAccessException(String message) {
        super(message, "UNAUTHORIZED_CUSTOMER_ACCESS");
    }
}
