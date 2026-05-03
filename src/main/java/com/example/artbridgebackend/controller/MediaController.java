package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.dto.MediaUploadResponse;
import com.example.artbridgebackend.entity.Media;
import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image", description = "Uploads an image file and returns its id and public URL")
    public ResponseEntity<MediaUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal JwtPrincipal principal) throws IOException {
        byte[] bytes = file.getBytes();
        Media media = storageService.upload(bytes, principal.getId());
        return ResponseEntity.ok(new MediaUploadResponse(media.getId(), storageService.buildPublicUrl(media)));
    }
}