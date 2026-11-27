package com.bazaarhub.backend.support.domain.model;

/**
 * Status lifecycle enum strictly matching MariaDB support_tickets.status enum:
 * ('OPEN','IN_PROGRESS','WAITING_FOR_CUSTOMER','RESOLVED','CLOSED').
 *
 * Note: Ticket assignment is tracked via the assigned_to foreign key (assigned_to != null)
 * with status transitioning to IN_PROGRESS.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_CUSTOMER,
    RESOLVED,
    CLOSED
}
