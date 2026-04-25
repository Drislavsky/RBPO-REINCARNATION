package com.example.autoservice.binary;

import com.example.autoservice.model.SignatureStatus;

import java.time.Instant;
import java.util.UUID;

public class BinaryManifestEntry {

    private final UUID id;
    private final SignatureStatus status;
    private final Instant updatedAt;
    private final long dataOffset;
    private final long dataLength;
    private final byte[] recordSignatureBytes;

    public BinaryManifestEntry(UUID id,
                               SignatureStatus status,
                               Instant updatedAt,
                               long dataOffset,
                               long dataLength,
                               byte[] recordSignatureBytes) {
        this.id = id;
        this.status = status;
        this.updatedAt = updatedAt;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
        this.recordSignatureBytes = recordSignatureBytes;
    }

    public UUID getId() {
        return id;
    }

    public SignatureStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getDataOffset() {
        return dataOffset;
    }

    public long getDataLength() {
        return dataLength;
    }

    public byte[] getRecordSignatureBytes() {
        return recordSignatureBytes;
    }
}