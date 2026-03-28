package com.danil.library.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LicenseTicketResponse(
        UUID licenseId,
        String licenseCode,
        Long userId,
        UUID productId,
        LocalDate validUntil
) {
}

