package com.bazaarhub.backend.support.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidTicketOperationException extends DomainException {

    public InvalidTicketOperationException(String message) {
        super(message, "INVALID_TICKET_OPERATION");
    }
}
