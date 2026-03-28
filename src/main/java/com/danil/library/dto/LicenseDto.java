package com.danil.library.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LicenseDto(
        UUID id,
        String code,
        /** null до первой активации (методичка). */
        Long userId,
        UUID productId,
        UUID typeId,
        LocalDate firstActivationDate,
        LocalDate endingDate,
        boolean blocked,
        Integer deviceCount,
        /** Владелец лицензии (users.id), методичка: owner_id. */
        Long ownerUserId,
        String description
) {
}
