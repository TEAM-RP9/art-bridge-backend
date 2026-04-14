package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.GoogleLoginRequest;
import com.example.artbridgebackend.dto.LoginRequest;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.enums.AccountStatus;
import com.example.artbridgebackend.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private GoogleLoginRequest googleLoginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("secret123");

        googleLoginRequest = new GoogleLoginRequest();
        googleLoginRequest.setIdToken("valid-google-token");
    }

    @Test
    void login_inactiveAccount_throwsDisabledException() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(AccountStatus.INACTIVE);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_lockedAccount_throwsLockedException() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(AccountStatus.LOCKED);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(LockedException.class, () -> authService.login(loginRequest));
    }

    @Test
    void googleLogin_inactiveAccount_throwsDisabledException() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setGoogleId("google-sub-123");
        user.setStatus(AccountStatus.INACTIVE);

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-123");
        payload.setEmail("test@example.com");
        payload.setEmailVerified(true);

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifier.verify(any(String.class))).thenReturn(idToken);
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> authService.googleLogin(googleLoginRequest));
    }

    @Test
    void googleLogin_lockedAccount_throwsLockedException() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setGoogleId("google-sub-123");
        user.setStatus(AccountStatus.LOCKED);

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-123");
        payload.setEmail("test@example.com");
        payload.setEmailVerified(true);

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifier.verify(any(String.class))).thenReturn(idToken);
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.of(user));

        assertThrows(LockedException.class, () -> authService.googleLogin(googleLoginRequest));
    }
}