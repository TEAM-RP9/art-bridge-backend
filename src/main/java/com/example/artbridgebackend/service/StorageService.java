package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.S3Properties;
import com.example.artbridgebackend.entity.Media;
import com.example.artbridgebackend.exception.InvalidImageException;
import com.example.artbridgebackend.repository.MediaRepository;
import com.example.artbridgebackend.repository.UserRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final S3Properties s3Properties;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    @Transactional
    public Media upload(byte[] bytes, Long ownerUserId) {
        String contentType = detectImageContentType(bytes);
        String key = UUID.randomUUID() + extensionFor(contentType);

        Media media = new Media();
        media.setObjectKey(key);
        media.setContentType(contentType);
        media.setSizeBytes((long) bytes.length);
        media.setOwnerUser(userRepository.getReferenceById(ownerUserId));
        mediaRepository.save(media);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(s3Properties.bucket())
                    .object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to object storage", e);
        }

        return media;
    }

    public String buildPublicUrl(Media media) {
        return s3Properties.publicBaseUrl() + "/" + s3Properties.bucket() + "/" + media.getObjectKey();
    }

    private String detectImageContentType(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        throw new InvalidImageException("File must be a valid PNG or JPEG image");
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> "";
        };
    }
}