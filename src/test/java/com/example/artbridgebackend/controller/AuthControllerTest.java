package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.SecurityConfig;
import com.example.artbridgebackend.dto.AuthResponse;
import com.example.artbridgebackend.repository.UserRepository;
import com.example.artbridgebackend.service.AuthService;
import com.example.artbridgebackend.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("placeholder")
                .tokenType("Bearer")
                .userId(1L)
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com", "password": "secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("placeholder"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication failed"));
    }

    @Test
    void login_inactiveAccount_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new DisabledException("Account is inactive"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com", "password": "secret123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication failed"));
    }

    @Test
    void login_lockedAccount_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new LockedException("Account is locked"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com", "password": "secret123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication failed"));
    }

    @Test
    void googleLogin_inactiveAccount_returns401() throws Exception {
        when(authService.googleLogin(any())).thenThrow(new DisabledException("Account is inactive"));

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication failed"));
    }

    @Test
    void googleLogin_lockedAccount_returns401() throws Exception {
        when(authService.googleLogin(any())).thenThrow(new LockedException("Account is locked"));

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication failed"));
    }

    @Test
    void login_withBlankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "", "password": "secret123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void login_withInvalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "secret123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void login_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleLogin_withValidToken_returns200() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("placeholder")
                .tokenType("Bearer")
                .userId(1L)
                .build();

        when(authService.googleLogin(any())).thenReturn(response);

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("placeholder"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void googleLogin_withValidTokenAndEmailLink_returns200() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("placeholder")
                .tokenType("Bearer")
                .userId(2L)
                .build();

        when(authService.googleLogin(any())).thenReturn(response);

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token-link"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    void googleLogin_withInvalidToken_returns401() throws Exception {
        when(authService.googleLogin(any())).thenThrow(new BadCredentialsException("Authentication failed"));

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "invalid-token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLogin_withUnverifiedEmail_returns401() throws Exception {
        when(authService.googleLogin(any())).thenThrow(new BadCredentialsException("Authentication failed"));

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "token-unverified-email"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLogin_withBlankIdToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void googleLogin_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
