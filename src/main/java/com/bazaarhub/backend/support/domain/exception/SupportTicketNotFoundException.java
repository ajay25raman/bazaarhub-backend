package com.bazaarhub.backend.support.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class SupportTicketNotFoundException extends ResourceNotFoundException {

    public SupportTicketNotFoundException(Long ticketId) {
        super(String.format("Support ticket with ID %d was not found", ticketId));
    }

    public SupportTicketNotFoundException(String ticketNumber) {
        super(String.format("Support ticket with number '%s' was not found", ticketNumber));
    }
}
