package com.example.autoservice.service;

import com.example.autoservice.config.SignatureProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Service
public class SignatureKeyStoreService {
    private final SignatureProperties props;
    private final ResourceLoader resourceLoader;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public SignatureKeyStoreService(SignatureProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(props.getKeyStoreType());
        try (InputStream is = resourceLoader.getResource(props.getKeyStorePath()).getInputStream()) {
            keyStore.load(is, props.getKeyStorePassword().toCharArray());
        }
        String keyPass = props.getKeyPassword() != null ? props.getKeyPassword() : props.getKeyStorePassword();
        this.privateKey = (PrivateKey) keyStore.getKey(props.getKeyAlias(), keyPass.toCharArray());
        Certificate cert = keyStore.getCertificate(props.getKeyAlias());
        this.publicKey = cert.getPublicKey();
    }

    public PrivateKey getPrivateKey() { return privateKey; }
    public PublicKey getPublicKey() { return publicKey; }
}