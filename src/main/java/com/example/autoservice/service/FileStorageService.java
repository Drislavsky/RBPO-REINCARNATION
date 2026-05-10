package com.example.autoservice.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final String bucket;
    private final Duration presignedUrlExpiry;

    public FileStorageService(MinioClient minioClient,
                              @Value("${minio.bucket}") String bucket,
                              @Value("${minio.presigned-url-expiry-minutes}") long presignedUrlExpiryMinutes) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.presignedUrlExpiry = Duration.ofMinutes(presignedUrlExpiryMinutes);
    }

    public void upload(String objectKey, MultipartFile file) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key must not be empty");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        try {
            ensurePrivateBucketExists();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(resolveContentType(file))
                                .build()
                );
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    public String getPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key must not be empty");
        }

        try {
            ensurePrivateBucketExists();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry((int) presignedUrlExpiry.toMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MinIO pre-signed URL", e);
        }
    }

    public Duration getPresignedUrlExpiry() {
        return presignedUrlExpiry;
    }

    private void ensurePrivateBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucket)
                            .build()
            );
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }
}