package com.example.support.importer;

import com.example.support.dto.CreateTicketRequest;

public class ParseResult {
    private final CreateTicketRequest ticket;
    private final int recordIndex;
    private final String error;

    public ParseResult(CreateTicketRequest ticket, int recordIndex, String error) {
        this.ticket = ticket;
        this.recordIndex = recordIndex;
        this.error = error;
    }

    public CreateTicketRequest ticket() {
        return ticket;
    }

    public int recordIndex() {
        return recordIndex;
    }

    public String error() {
        return error;
    }

    public boolean isSuccess() {
        return error == null;
    }
}
