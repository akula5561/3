package com.danil.library.dto;

public record ActivateLicenseRequest(
        String activationKey,
        String deviceMac,
        String deviceName
) {
}

