package com.danil.library.dto;

public record TicketResponse(
        Ticket ticket,
        String signature
) {
}

