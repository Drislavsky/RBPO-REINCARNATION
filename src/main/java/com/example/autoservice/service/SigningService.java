package com.example.autoservice.service;

import org.springframework.stereotype.Service;

import java.security.Signature;
import java.util.Base64;

@Service
public class SigningService {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final SignatureKeyStoreService keyStoreService;
    private final JsonCanonicalizer canonicalizer;

    public SigningService(SignatureKeyStoreService keyStoreService, JsonCanonicalizer canonicalizer) {
        this.keyStoreService = keyStoreService;
        this.canonicalizer = canonicalizer;
    }

    public String sign(Object payload) {
        try {
            byte[] data = canonicalizer.canonicalize(payload);
            return Base64.getEncoder().encodeToString(signBytes(data));
        } catch (Exception e) {
            throw new RuntimeException("Digital signature generation failed", e);
        }
    }

    public byte[] signBytes(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data for signing must not be null");
        }

        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initSign(keyStoreService.getPrivateKey());
            sig.update(data);
            return sig.sign();
        } catch (Exception e) {
            throw new RuntimeException("Digital signature generation failed", e);
        }
    }
}