package com.danil.library.signature;

import java.security.PrivateKey;

/**
 * Поставщик ключевого материала:
 * - приватный ключ для подписи;
 * - публичные данные для проверки подписи на клиенте.
 */
public interface KeyProvider {
    PrivateKey getSigningKey();

    VerificationInfo getVerificationInfo();
}
