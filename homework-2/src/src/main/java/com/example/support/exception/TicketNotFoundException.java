package com.example.support.exception;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {

    private final UUID ticketId;

    public TicketNotFoundException(UUID id) {
        super("Ticket " + id + " not found");
        this.ticketId = id;
    }

    public UUID getTicketId() {
        return ticketId;
    }
}
