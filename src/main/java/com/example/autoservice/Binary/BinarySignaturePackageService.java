package com.example.autoservice.binary;

import com.example.autoservice.model.MalwareSignature;
import com.example.autoservice.model.SignatureStatus;
import com.example.autoservice.repository.MalwareSignatureRepository;
import com.example.autoservice.service.SigningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class BinarySignaturePackageService {

    private static final String MANIFEST_MAGIC = "MF-STAVISKIY";
    private static final String DATA_MAGIC = "DB-STAVISKIY";
    private static final int FORMAT_VERSION = 1;

    private final MalwareSignatureRepository signatureRepository;
    private final SigningService signingService;

    public BinarySignaturePackageService(MalwareSignatureRepository signatureRepository,
                                         SigningService signingService) {
        this.signatureRepository = signatureRepository;
        this.signingService = signingService;
    }

    @Transactional(readOnly = true)
    public BinarySignaturePackage buildFullPackage() {
        List<MalwareSignature> signatures = signatureRepository.findAllByStatusOrderByUpdatedAtDesc(SignatureStatus.ACTUAL);
        return buildPackage(signatures, BinaryExportType.FULL, -1L);
    }

    @Transactional(readOnly = true)
    public BinarySignaturePackage buildIncrementPackage(Instant since) {
        if (since == null) {
            throw new IllegalArgumentException("Parameter 'since' is required");
        }
        List<MalwareSignature> signatures = signatureRepository.findAllByUpdatedAtAfterOrderByUpdatedAtDesc(since);
        return buildPackage(signatures, BinaryExportType.INCREMENT, since.toEpochMilli());
    }

    @Transactional(readOnly = true)
    public BinarySignaturePackage buildByIdsPackage(List<UUID> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("IDs list is required");
        }
        if (ids.isEmpty()) {
            return buildPackage(List.of(), BinaryExportType.BY_IDS, -1L);
        }
        List<MalwareSignature> signatures = signatureRepository.findAllByIdIn(ids);
        return buildPackage(signatures, BinaryExportType.BY_IDS, -1L);
    }

    private BinarySignaturePackage buildPackage(List<MalwareSignature> signatures,
                                                BinaryExportType exportType,
                                                long sinceEpochMillis) {
        DataBuildResult dataBuildResult = buildData(signatures);
        byte[] dataSha256 = sha256(dataBuildResult.dataBytes());
        byte[] manifestBytes = buildManifest(
                dataBuildResult.entries(),
                exportType,
                Instant.now().toEpochMilli(),
                sinceEpochMillis,
                dataSha256
        );
        return new BinarySignaturePackage(manifestBytes, dataBuildResult.dataBytes());
    }

    private DataBuildResult buildData(List<MalwareSignature> signatures) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeAscii(DATA_MAGIC);
        writer.writeUInt16(FORMAT_VERSION);
        writer.writeUInt32(signatures.size());

        int payloadStart = writer.size();
        List<BinaryManifestEntry> entries = new ArrayList<>();

        for (MalwareSignature signature : signatures) {
            long dataOffset = writer.size() - payloadStart;
            byte[] recordBytes = buildDataRecord(signature);
            writer.writeBytes(recordBytes);
            long dataLength = recordBytes.length;

            entries.add(new BinaryManifestEntry(
                    signature.getId(),
                    signature.getStatus(),
                    signature.getUpdatedAt(),
                    dataOffset,
                    dataLength,
                    decodeRecordSignature(signature.getDigitalSignatureBase64())
            ));
        }

        return new DataBuildResult(writer.toByteArray(), entries);
    }

    private byte[] buildDataRecord(MalwareSignature signature) {
        validateSignatureForBinaryExport(signature);

        BinaryWriter writer = new BinaryWriter();
        writer.writeUtf8String(signature.getThreatName());
        writer.writeByteArray(hexToBytes(signature.getFirstBytesHex()));
        writer.writeByteArray(hexToBytes(signature.getRemainderHashHex()));
        writer.writeInt64(signature.getRemainderLength());
        writer.writeUtf8String(signature.getFileType());
        writer.writeInt64(signature.getOffsetStart());
        writer.writeInt64(signature.getOffsetEnd());
        return writer.toByteArray();
    }

    private byte[] buildManifest(List<BinaryManifestEntry> entries,
                                 BinaryExportType exportType,
                                 long generatedAtEpochMillis,
                                 long sinceEpochMillis,
                                 byte[] dataSha256) {
        if (dataSha256 == null || dataSha256.length != 32) {
            throw new IllegalArgumentException("dataSha256 must contain exactly 32 bytes");
        }

        BinaryWriter unsignedWriter = new BinaryWriter();
        unsignedWriter.writeAscii(MANIFEST_MAGIC);
        unsignedWriter.writeUInt16(FORMAT_VERSION);
        unsignedWriter.writeByte(exportType.getCode());
        unsignedWriter.writeInt64(generatedAtEpochMillis);
        unsignedWriter.writeInt64(sinceEpochMillis);
        unsignedWriter.writeUInt32(entries.size());
        unsignedWriter.writeBytes(dataSha256);

        for (BinaryManifestEntry entry : entries) {
            unsignedWriter.writeUuid(entry.getId());
            unsignedWriter.writeByte(statusCode(entry.getStatus()));
            unsignedWriter.writeInt64(entry.getUpdatedAt().toEpochMilli());
            unsignedWriter.writeInt64(entry.getDataOffset());
            unsignedWriter.writeUInt32(entry.getDataLength());
            unsignedWriter.writeByteArray(entry.getRecordSignatureBytes());
        }

        byte[] unsignedManifest = unsignedWriter.toByteArray();
        byte[] manifestSignature = signingService.signBytes(unsignedManifest);

        BinaryWriter signedWriter = new BinaryWriter();
        signedWriter.writeBytes(unsignedManifest);
        signedWriter.writeByteArray(manifestSignature);
        return signedWriter.toByteArray();
    }

    private void validateSignatureForBinaryExport(MalwareSignature signature) {
        if (signature.getId() == null) {
            throw new IllegalStateException("Signature id must not be null");
        }
        if (signature.getUpdatedAt() == null) {
            throw new IllegalStateException("updatedAt must not be null for signature " + signature.getId());
        }
        if (signature.getStatus() == null) {
            throw new IllegalStateException("status must not be null for signature " + signature.getId());
        }
        if (signature.getRemainderLength() == null) {
            throw new IllegalStateException("remainderLength must not be null for signature " + signature.getId());
        }
        if (signature.getOffsetStart() == null || signature.getOffsetEnd() == null) {
            throw new IllegalStateException("offsetStart and offsetEnd must not be null for signature " + signature.getId());
        }
        if (signature.getOffsetEnd() < signature.getOffsetStart()) {
            throw new IllegalStateException("offsetEnd must be >= offsetStart for signature " + signature.getId());
        }
        if (signature.getDigitalSignatureBase64() == null || signature.getDigitalSignatureBase64().isBlank()) {
            throw new IllegalStateException("digitalSignatureBase64 must not be empty for signature " + signature.getId());
        }
    }

    private int statusCode(SignatureStatus status) {
        if (status == SignatureStatus.ACTUAL) {
            return 1;
        }
        if (status == SignatureStatus.DELETED) {
            return 2;
        }
        throw new IllegalArgumentException("Unsupported signature status: " + status);
    }

    private byte[] decodeRecordSignature(String base64) {
        return Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256", e);
        }
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isBlank()) {
            return new byte[0];
        }
        String normalized = hex.trim();
        if (normalized.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex value must contain full bytes");
        }

        byte[] result = new byte[normalized.length() / 2];
        for (int i = 0; i < normalized.length(); i += 2) {
            int high = Character.digit(normalized.charAt(i), 16);
            int low = Character.digit(normalized.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Hex value contains invalid symbol");
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }

    private record DataBuildResult(byte[] dataBytes, List<BinaryManifestEntry> entries) {
    }
}