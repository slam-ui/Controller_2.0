package com.example.shop.signature;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SigningService {

    private final SignatureKeyStoreService keyStoreService;
    private final JsonCanonicalizer jsonCanonicalizer;

    /**
     * Подписывает payload алгоритмом SHA256withRSA.
     * 1. Канонизирует JSON по RFC 8785
     * 2. Получает UTF-8 байты
     * 3. Подписывает приватным ключом
     * 4. Возвращает Base64-строку
     */
    public String sign(Object payload) {
        try {
            // Шаг 1-2: канонизация → UTF-8 байты
            String canonical = jsonCanonicalizer.canonizeJson(payload);
            byte[] canonicalBytes = canonical.getBytes(StandardCharsets.UTF_8);

            // Шаг 3: подпись SHA256withRSA
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyStoreService.getPrivateKey());
            signature.update(canonicalBytes);
            byte[] signatureBytes = signature.sign();

            // Шаг 4: Base64
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign payload", e);
        }
    }

    /**
     * Проверяет подпись на стороне сервера (для тестирования).
     */
    public boolean verify(Object payload, String signatureBase64) {
        try {
            String canonical = jsonCanonicalizer.canonizeJson(payload);
            byte[] canonicalBytes = canonical.getBytes(StandardCharsets.UTF_8);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(keyStoreService.getPublicKey());
            signature.update(canonicalBytes);

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }
}
