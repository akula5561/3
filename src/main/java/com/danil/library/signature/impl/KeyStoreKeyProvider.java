package com.danil.library.signature.impl;

import com.danil.library.signature.KeyProvider;
import com.danil.library.signature.SignatureErrorCode;
import com.danil.library.signature.SignatureModuleException;
import com.danil.library.signature.SignatureProperties;
import com.danil.library.signature.VerificationInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Base64;

@Component
/** Загружает ключи из keystore и кеширует их в памяти после первого чтения. */
public class KeyStoreKeyProvider implements KeyProvider {

    private final SignatureProperties properties;
    private final ResourceLoader resourceLoader;

    private volatile LoadedKeyMaterial cache;

    public KeyStoreKeyProvider(SignatureProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public PrivateKey getSigningKey() {
        // Для подписи нужен приватный ключ из keystore.
        return getOrLoad().privateKey();
    }

    @Override
    public VerificationInfo getVerificationInfo() {
        try {
            // Для клиента отдаём алгоритм + публичный ключ + сертификат.
            LoadedKeyMaterial material = getOrLoad();
            return new VerificationInfo(
                    properties.getAlgorithm(),
                    Base64.getEncoder().encodeToString(material.certificate().getPublicKey().getEncoded()),
                    Base64.getEncoder().encodeToString(material.certificate().getEncoded())
            );
        } catch (SignatureModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureModuleException(
                    SignatureErrorCode.KEY_PROVIDER_ERROR,
                    "Failed to build verification info",
                    e
            );
        }
    }

    private LoadedKeyMaterial getOrLoad() {
        // Быстрый путь: если уже загружали ключи, возвращаем кеш.
        LoadedKeyMaterial current = cache;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            // Потокобезопасная ленивaя инициализация.
            if (cache == null) {
                cache = loadMaterial();
            }
            return cache;
        }
    }

    private LoadedKeyMaterial loadMaterial() {
        // Проверяем, что минимально нужные настройки присутствуют.
        validateRequiredProperties();

        char[] storePassword = properties.getKeyStorePassword().toCharArray();
        char[] keyPassword = resolveKeyPassword();

        try (InputStream inputStream = openKeyStoreStream()) {
            // Открываем keystore, достаём ключ по alias и сертификат для проверки подписи.
            KeyStore keyStore = KeyStore.getInstance(properties.getKeyStoreType());
            keyStore.load(inputStream, storePassword);

            Key key = keyStore.getKey(properties.getKeyAlias(), keyPassword);
            if (!(key instanceof PrivateKey privateKey)) {
                throw new SignatureModuleException(
                        SignatureErrorCode.KEY_PROVIDER_ERROR,
                        "Configured alias does not contain a private key: " + properties.getKeyAlias()
                );
            }

            Certificate certificate = keyStore.getCertificate(properties.getKeyAlias());
            if (certificate == null) {
                throw new SignatureModuleException(
                        SignatureErrorCode.KEY_PROVIDER_ERROR,
                        "Certificate not found for alias: " + properties.getKeyAlias()
                );
            }

            return new LoadedKeyMaterial(privateKey, certificate);
        } catch (SignatureModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureModuleException(
                    SignatureErrorCode.KEY_PROVIDER_ERROR,
                    "Failed to load signing key from keystore",
                    e
            );
        }
    }

    private InputStream openKeyStoreStream() throws Exception {
        String path = properties.getKeyStorePath();
        Resource resource;

        // Поддерживаем classpath:/file:/ и обычный путь файловой системы.
        if (path.startsWith("classpath:") || path.startsWith("file:")) {
            resource = resourceLoader.getResource(path);
        } else {
            resource = resourceLoader.getResource("file:" + path);
        }

        if (!resource.exists()) {
            throw new SignatureModuleException(
                    SignatureErrorCode.KEY_PROVIDER_ERROR,
                    "Keystore file not found: " + path
            );
        }

        return resource.getInputStream();
    }

    private char[] resolveKeyPassword() {
        String keyPassword = properties.getKeyPassword();
        // Поведение как у keytool: если key-password не задан, используем store-password.
        if (!StringUtils.hasText(keyPassword)) {
            return properties.getKeyStorePassword().toCharArray();
        }
        return keyPassword.toCharArray();
    }

    private void validateRequiredProperties() {
        if (!StringUtils.hasText(properties.getKeyStorePath())) {
            throw new SignatureModuleException(SignatureErrorCode.KEY_PROVIDER_ERROR, "signature.key-store-path is required");
        }
        if (!StringUtils.hasText(properties.getKeyStorePassword())) {
            throw new SignatureModuleException(SignatureErrorCode.KEY_PROVIDER_ERROR, "signature.key-store-password is required");
        }
        if (!StringUtils.hasText(properties.getKeyAlias())) {
            throw new SignatureModuleException(SignatureErrorCode.KEY_PROVIDER_ERROR, "signature.key-alias is required");
        }
    }

    private record LoadedKeyMaterial(PrivateKey privateKey, Certificate certificate) {
    }
}
