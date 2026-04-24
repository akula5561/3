package com.danil.library.signature;

/**
 * Данные для клиентской проверки подписи:
 * алгоритм, публичный ключ и сертификат (в Base64).
 */
public record VerificationInfo(
        String algorithm,
        String publicKeyBase64,
        String certificateBase64
) {
}
