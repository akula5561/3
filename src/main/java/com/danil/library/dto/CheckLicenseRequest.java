package com.danil.library.dto;

import java.util.UUID;

public record CheckLicenseRequest(
        String deviceMac,
        UUID productId
) {
}

