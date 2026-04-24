package com.danil.library.signature;

/** Фасад подписи: принимает payload и возвращает подпись в Base64. */
public interface SigningService {
    String sign(Object payload);
}
