package com.example.autoservice.binary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BinaryWriter {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public void writeByte(int value) {
        output.write(value & 0xFF);
    }

    public void writeBytes(byte[] value) {
        if (value == null) {
            return;
        }
        output.writeBytes(value);
    }

    public void writeAscii(String value) {
        writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    public void writeUtf8String(String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        writeUInt32(bytes.length);
        writeBytes(bytes);
    }

    public void writeByteArray(byte[] value) {
        byte[] bytes = value == null ? new byte[0] : value;
        writeUInt32(bytes.length);
        writeBytes(bytes);
    }

    public void writeUInt16(int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("uint16 value is out of range: " + value);
        }
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    public void writeUInt32(long value) {
        if (value < 0 || value > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("uint32 value is out of range: " + value);
        }
        output.write((int) ((value >>> 24) & 0xFF));
        output.write((int) ((value >>> 16) & 0xFF));
        output.write((int) ((value >>> 8) & 0xFF));
        output.write((int) (value & 0xFF));
    }

    public void writeInt64(long value) {
        output.write((int) ((value >>> 56) & 0xFF));
        output.write((int) ((value >>> 48) & 0xFF));
        output.write((int) ((value >>> 40) & 0xFF));
        output.write((int) ((value >>> 32) & 0xFF));
        output.write((int) ((value >>> 24) & 0xFF));
        output.write((int) ((value >>> 16) & 0xFF));
        output.write((int) ((value >>> 8) & 0xFF));
        output.write((int) (value & 0xFF));
    }

    public void writeUuid(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("UUID value must not be null");
        }
        writeInt64(value.getMostSignificantBits());
        writeInt64(value.getLeastSignificantBits());
    }

    public int size() {
        return output.size();
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }
}