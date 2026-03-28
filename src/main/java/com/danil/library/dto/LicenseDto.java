package com.danil.library.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LicenseDto(
        UUID id,
        String code,
        Long userId,
        UUID productId,
        UUID typeId,
        LocalDate firstActivationDate,
        LocalDate endingDate,
        boolean blocked,
        Integer deviceCount,
        UUID ownerId,
        String description
) {
}

