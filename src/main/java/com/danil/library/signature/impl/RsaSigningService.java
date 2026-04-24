package com.danil.library.signature.impl;

import com.danil.library.signature.CanonicalizationService;
import com.danil.library.signature.KeyProvider;
import com.danil.library.signature.SignatureErrorCode;
import com.danil.library.signature.SignatureModuleException;
import com.danil.library.signature.SignatureProperties;
import com.danil.library.signature.SigningService;
import org.springframework.stereotype.Service;

import java.security.Signature;
import java.util.Base64;

@Service
/**
 * Подписывает payload: canonicalize -> SHA256withRSA -> Base64.
 * Это главный фасад криптографической операции в модуле ЭЦП.
 */
public class RsaSigningService implements SigningService {

    private final CanonicalizationService canonicalizationService;
    private final KeyProvider keyProvider;
    private final SignatureProperties signatureProperties;

    public RsaSigningService(
            CanonicalizationService canonicalizationService,
            KeyProvider keyProvider,
            SignatureProperties signatureProperties
    ) {
        this.canonicalizationService = canonicalizationService;
        this.keyProvider = keyProvider;
        this.signatureProperties = signatureProperties;
    }

    @Override
    public String sign(Object payload) {
        try {
            // 1) Получаем канонические байты payload.
            byte[] canonicalBytes = canonicalizationService.canonicalize(payload);
            // 2) Создаём объект подписи и инициализируем приватным ключом.
            Signature signature = Signature.getInstance(signatureProperties.getAlgorithm());
            signature.initSign(keyProvider.getSigningKey());
            // 3) Подписываем байты.
            signature.update(canonicalBytes);
            byte[] signatureBytes = signature.sign();
            // 4) Возвращаем подпись в удобном текстовом формате Base64.
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (SignatureModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureModuleException(
                    SignatureErrorCode.SIGN_OPERATION_FAILED,
                    "Failed to sign canonical payload",
                    e
            );
        }
    }
}
