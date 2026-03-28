package com.danil.library.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Ticket(
        Instant serverTime,
        long ttlSeconds,
        LocalDate activationDate,
        LocalDate expirationDate,
        Long userId,
        UUID deviceId,
        boolean blocked
) {
}

