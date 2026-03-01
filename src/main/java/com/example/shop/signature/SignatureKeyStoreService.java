package com.example.shop.signature;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Service
@RequiredArgsConstructor
public class SignatureKeyStoreService {

    private final SignatureProperties properties;
    private final ResourceLoader resourceLoader;

    // Кешируем ключи — не читаем keystore при каждом запросе
    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;

    public PrivateKey getPrivateKey() {
        if (privateKey != null) return privateKey;
        synchronized (this) {
            if (privateKey == null) privateKey = loadPrivateKey();
            return privateKey;
        }
    }

    public PublicKey getPublicKey() {
        if (publicKey != null) return publicKey;
        synchronized (this) {
            if (publicKey == null) publicKey = loadPublicKey();
            return publicKey;
        }
    }

    private PrivateKey loadPrivateKey() {
        try {
            KeyStore ks = loadKeyStore();
            String alias = properties.getKeyAlias();
            char[] keyPass = resolveKeyPassword();
            java.security.Key key = ks.getKey(alias, keyPass);
            if (key == null) throw new IllegalStateException("Key not found for alias: " + alias);
            if (!(key instanceof PrivateKey pk)) throw new IllegalStateException("Not a private key: " + alias);
            return pk;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }

    private PublicKey loadPublicKey() {
        try {
            KeyStore ks = loadKeyStore();
            Certificate cert = ks.getCertificate(properties.getKeyAlias());
            if (cert == null) throw new IllegalStateException("Certificate not found for alias: " + properties.getKeyAlias());
            return cert.getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load public key", e);
        }
    }

    private KeyStore loadKeyStore() {
        String path = properties.getKeyStorePath();
        String type = properties.getKeyStoreType() == null ? "JKS" : properties.getKeyStoreType();
        char[] storePass = properties.getKeyStorePassword().toCharArray();
        try (InputStream is = openStream(path)) {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(is, storePass);
            return ks;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load keystore: " + path, e);
        }
    }

    private char[] resolveKeyPassword() {
        String kp = properties.getKeyPassword();
        if (kp != null && !kp.isBlank()) return kp.toCharArray();
        return properties.getKeyStorePassword().toCharArray();
    }

    private InputStream openStream(String path) throws Exception {
        String lower = path.toLowerCase().trim();
        if (lower.startsWith("classpath:") || lower.startsWith("file:")) {
            Resource resource = resourceLoader.getResource(path.trim());
            if (!resource.exists()) throw new IllegalStateException("Keystore not found: " + path);
            return resource.getInputStream();
        }
        return Files.newInputStream(Path.of(path));
    }
}
