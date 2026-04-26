package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.S3Properties;
import com.example.artbridgebackend.entity.Media;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.exception.InvalidImageException;
import com.example.artbridgebackend.repository.MediaRepository;
import com.example.artbridgebackend.repository.UserRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    @Mock
    private MinioClient minioClient;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UserRepository userRepository;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        S3Properties s3Properties = new S3Properties(
                "http://localhost:9000", "access", "secret", "photos", "http://localhost/media");
        storageService = new StorageService(minioClient, s3Properties, mediaRepository, userRepository);
    }

    @Test
    void upload_validPngBytes_uploadsAndPersistsAndReturnsUrl() throws Exception {
        User user = new User();
        when(userRepository.getReferenceById(42L)).thenReturn(user);
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        String url = storageService.upload(PNG_BYTES, 42L);

        verify(minioClient).putObject(any(PutObjectArgs.class));
        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(mediaCaptor.capture());
        Media saved = mediaCaptor.getValue();
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getSizeBytes()).isEqualTo((long) PNG_BYTES.length);
        assertThat(saved.getOwnerUser()).isSameAs(user);
        assertThat(url).startsWith("http://localhost/media/photos/");
        assertThat(url).endsWith(".png");
    }

    @Test
    void upload_validJpegBytes_uses_image_jpeg_andJpgExtension() throws Exception {
        User user = new User();
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        String url = storageService.upload(JPEG_BYTES, 1L);

        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(mediaCaptor.capture());
        assertThat(mediaCaptor.getValue().getContentType()).isEqualTo("image/jpeg");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    void upload_textBytesRenamedPng_throwsInvalidImageException_andSkipsMinio() {
        byte[] textBytes = "not an image".getBytes();

        assertThatThrownBy(() -> storageService.upload(textBytes, 1L))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("File must be a valid PNG or JPEG image");

        verifyNoInteractions(minioClient, mediaRepository);
    }

    @Test
    void upload_s3PutFails_savesMediaThenRethrows() throws Exception {
        User user = new User();
        when(userRepository.getReferenceById(42L)).thenReturn(user);
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("S3 down"));

        assertThatThrownBy(() -> storageService.upload(PNG_BYTES, 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to upload file to object storage");

        InOrder inOrder = inOrder(mediaRepository, minioClient);
        inOrder.verify(mediaRepository).save(any(Media.class));
        inOrder.verify(minioClient).putObject(any(PutObjectArgs.class));
    }
}