package com.danil.library.controller;

import com.danil.library.signature.KeyProvider;
import com.danil.library.signature.VerificationInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signature")
/**
 * Публичные данные для верификации подписи на клиенте.
 * Секреты/приватный ключ здесь не возвращаются.
 */
public class SignatureController {

    private final KeyProvider keyProvider;

    public SignatureController(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/verification-info")
    public VerificationInfo verificationInfo() {
        // Клиент использует эти данные, чтобы проверить Ticket.signature.
        return keyProvider.getVerificationInfo();
    }
}
