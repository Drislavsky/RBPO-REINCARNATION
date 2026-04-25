package com.example.autoservice.binary;

public class BinarySignaturePackage {

    private final byte[] manifestBytes;
    private final byte[] dataBytes;

    public BinarySignaturePackage(byte[] manifestBytes, byte[] dataBytes) {
        this.manifestBytes = manifestBytes;
        this.dataBytes = dataBytes;
    }

    public byte[] getManifestBytes() {
        return manifestBytes;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }
}