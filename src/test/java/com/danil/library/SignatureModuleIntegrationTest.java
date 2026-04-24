package com.danil.library;

import com.danil.library.signature.CanonicalizationService;
import com.danil.library.signature.KeyProvider;
import com.danil.library.signature.SigningService;
import com.danil.library.signature.VerificationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SignatureModuleIntegrationTest {

    @Autowired
    private SigningService signingService;

    @Autowired
    private CanonicalizationService canonicalizationService;

    @Autowired
    private KeyProvider keyProvider;

    @Test
    void signsDeterministicallyAndCanBeVerifiedWithPublicKey() throws Exception {
        Map<String, Object> payload1 = new LinkedHashMap<>();
        payload1.put("b", 2);
        payload1.put("a", "value");

        Map<String, Object> payload2 = new LinkedHashMap<>();
        payload2.put("a", "value");
        payload2.put("b", 2);

        String signature1 = signingService.sign(payload1);
        String signature2 = signingService.sign(payload2);

        assertEquals(signature1, signature2, "Canonicalization must make signature deterministic");

        VerificationInfo verificationInfo = keyProvider.getVerificationInfo();

        byte[] canonicalBytes = canonicalizationService.canonicalize(payload1);
        byte[] signatureBytes = Base64.getDecoder().decode(signature1);

        Signature verifier = Signature.getInstance(verificationInfo.algorithm());
        verifier.initVerify(extractPublicKey(verificationInfo));
        verifier.update(canonicalBytes);

        assertTrue(verifier.verify(signatureBytes), "Signature must be verifiable by public key");
    }

    private PublicKey extractPublicKey(VerificationInfo verificationInfo) throws Exception {
        byte[] certBytes = Base64.getDecoder().decode(verificationInfo.certificateBase64());
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certificateFactory
                .generateCertificate(new java.io.ByteArrayInputStream(certBytes));

        byte[] publicKeyBytes = Base64.getDecoder().decode(verificationInfo.publicKeyBase64());
        PublicKey fromInfo = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        // Дополнительная проверка согласованности данных в VerificationInfo.
        assertEquals(
                Base64.getEncoder().encodeToString(certificate.getPublicKey().getEncoded()),
                Base64.getEncoder().encodeToString(fromInfo.getEncoded())
        );

        return fromInfo;
    }
}
