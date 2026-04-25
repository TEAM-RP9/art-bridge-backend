package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.S3Properties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final S3Properties s3Properties;

    public String upload(String originalFilename, InputStream data, long size, String contentType) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String key = UUID.randomUUID() + extension;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(s3Properties.bucket())
                    .object(key)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to object storage", e);
        }

        return s3Properties.publicBaseUrl() + "/" + s3Properties.bucket() + "/" + key;
    }
}
