package com.example.autoservice.service;

import org.springframework.stereotype.Service;

import java.security.Signature;
import java.util.Base64;

@Service
public class SigningService {
    private final SignatureKeyStoreService keyStoreService;
    private final JsonCanonicalizer canonicalizer;

    public SigningService(SignatureKeyStoreService keyStoreService, JsonCanonicalizer canonicalizer) {
        this.keyStoreService = keyStoreService;
        this.canonicalizer = canonicalizer;
    }

    public String sign(Object payload) {
        try {
            byte[] data = canonicalizer.canonicalize(payload);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(keyStoreService.getPrivateKey());
            sig.update(data);
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new RuntimeException("Digital signature generation failed", e);
        }
    }
}