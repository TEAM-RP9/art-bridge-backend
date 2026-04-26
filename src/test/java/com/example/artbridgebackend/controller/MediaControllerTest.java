package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.config.SecurityConfig;
import com.example.artbridgebackend.exception.InvalidImageException;
import com.example.artbridgebackend.repository.UserRepository;
import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.AuthService;
import com.example.artbridgebackend.service.StorageService;
import com.example.artbridgebackend.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-value-of-exactly-32bytes!",
        "jwt.access-token-ttl=15m",
        "jwt.refresh-token-ttl=30d",
        "jwt.access-cookie-name=jwt",
        "jwt.refresh-cookie-name=refresh"
})
class MediaControllerTest {

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Test
    void upload_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/media/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(storageService);
    }

    @Test
    void upload_validPng_returns200WithUrl() throws Exception {
        when(storageService.upload(any(byte[].class), any(Long.class)))
                .thenReturn("http://localhost/media/photos/uuid.png");

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/media/upload")
                        .file(file)
                        .with(csrf())
                        .with(authentication(token(new JwtPrincipal(42L, "test@example.com", "ARTIST")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/media/photos/uuid.png"));
    }

    @Test
    void upload_textRenamedAsPng_returns400() throws Exception {
        when(storageService.upload(any(byte[].class), any(Long.class)))
                .thenThrow(new InvalidImageException("File must be a valid PNG or JPEG image"));

        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png",
                "not an image".getBytes());

        mockMvc.perform(multipart("/media/upload")
                        .file(file)
                        .with(csrf())
                        .with(authentication(token(new JwtPrincipal(42L, "test@example.com", "ARTIST")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("File must be a valid PNG or JPEG image"));
    }

    @Test
    void upload_withoutCsrf_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/media/upload")
                        .file(file)
                        .with(authentication(token(new JwtPrincipal(42L, "test@example.com", "ARTIST")))))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken token(JwtPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()))
        );
    }
}