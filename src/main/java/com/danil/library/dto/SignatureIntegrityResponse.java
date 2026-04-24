package com.danil.library.dto;

import java.util.UUID;

public record SignatureIntegrityResponse(
        UUID signatureId,
        boolean valid
) {
}
