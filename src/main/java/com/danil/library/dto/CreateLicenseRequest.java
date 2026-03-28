package com.danil.library.dto;

import java.util.UUID;

public record CreateLicenseRequest(
        UUID productId,
        UUID typeId,
        Long ownerUserId,
        Integer deviceCount,
        String description
) {
}

