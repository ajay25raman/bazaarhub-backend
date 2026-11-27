package com.bazaarhub.backend.support.domain.model;

/**
 * Priority classification strictly matching MariaDB support_tickets.priority enum:
 * ('LOW','MEDIUM','HIGH','URGENT').
 */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
