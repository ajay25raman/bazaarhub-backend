package com.bazaarhub.backend.support.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedTicketAccessException extends DomainException {

    public UnauthorizedTicketAccessException(Long userId, Long ticketId) {
        super(String.format("User %d is not authorized to access support ticket %d", userId, ticketId), "UNAUTHORIZED_TICKET_ACCESS");
    }

    public UnauthorizedTicketAccessException(String message) {
        super(message, "UNAUTHORIZED_TICKET_ACCESS");
    }
}
