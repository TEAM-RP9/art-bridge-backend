package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.config.SecurityConfig;
import com.example.artbridgebackend.entity.Role;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.repository.UserRepository;
import com.example.artbridgebackend.service.AuthService;
import com.example.artbridgebackend.service.RefreshTokenService;
import com.example.artbridgebackend.service.RefreshTokenService.IssuedRefreshToken;
import com.example.artbridgebackend.service.RefreshTokenService.RotationResult;
import com.example.artbridgebackend.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-value-of-exactly-32bytes!",
        "jwt.access-token-ttl=15m",
        "jwt.refresh-token-ttl=30d",
        "jwt.access-cookie-name=jwt",
        "jwt.refresh-cookie-name=refresh"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    private User seededUser;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        seededUser = new User();
        seededUser.setId(42L);
        seededUser.setEmail("test@example.com");
        seededUser.setRole(role);
    }

    @Test
    void login_withValidCredentials_setsBothCookiesAndReturnsIdentity() throws Exception {
        when(authService.login(any())).thenReturn(seededUser);
        when(authService.generateToken(seededUser)).thenReturn("test.jwt.token");
        when(refreshTokenService.issue(seededUser)).thenReturn(
                new IssuedRefreshToken("raw-refresh", Instant.now().plus(30, ChronoUnit.DAYS)));

        MvcResult result = mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com", "password": "secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        assertThat(setCookie(result, "jwt")).contains("jwt=test.jwt.token")
                .contains("HttpOnly").contains("Secure").contains("SameSite=Strict");
        assertThat(setCookie(result, "refresh")).contains("refresh=raw-refresh")
                .contains("Path=/auth/refresh").contains("HttpOnly").contains("Secure")
                .contains("SameSite=Strict");
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void login_withBlankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "", "password": "secret123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void login_withInvalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "secret123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void login_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com", "password": "secret123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void googleLogin_withValidToken_setsBothCookiesAndReturnsIdentity() throws Exception {
        when(authService.googleLogin(any())).thenReturn(seededUser);
        when(authService.generateToken(seededUser)).thenReturn("test.jwt.token");
        when(refreshTokenService.issue(seededUser)).thenReturn(
                new IssuedRefreshToken("raw-refresh", Instant.now().plus(30, ChronoUnit.DAYS)));

        MvcResult result = mockMvc.perform(post("/auth/oauth/google").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "valid-google-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        assertThat(setCookie(result, "jwt")).contains("jwt=test.jwt.token");
        assertThat(setCookie(result, "refresh")).contains("refresh=raw-refresh");
    }

    @Test
    void googleLogin_withInvalidToken_returns401() throws Exception {
        when(authService.googleLogin(any())).thenThrow(new BadCredentialsException("Authentication failed"));

        mockMvc.perform(post("/auth/oauth/google").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "invalid-token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLogin_withBlankIdToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/oauth/google").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void googleLogin_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/oauth/google").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withValidRefreshCookie_rotatesAndSetsBothCookies() throws Exception {
        Instant newExpiry = Instant.now().plus(30, ChronoUnit.DAYS);
        when(refreshTokenService.rotate("old-refresh")).thenReturn(
                new RotationResult(seededUser, new IssuedRefreshToken("new-refresh", newExpiry)));
        when(authService.generateToken(seededUser)).thenReturn("new.jwt.token");

        MvcResult result = mockMvc.perform(post("/auth/refresh").with(csrf())
                        .cookie(new Cookie("refresh", "old-refresh")))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(setCookie(result, "jwt")).contains("jwt=new.jwt.token");
        assertThat(setCookie(result, "refresh")).contains("refresh=new-refresh")
                .contains("Path=/auth/refresh");
    }

    @Test
    void refresh_withNoRefreshCookie_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withInvalidRefreshCookie_returns401() throws Exception {
        when(refreshTokenService.rotate("bogus"))
                .thenThrow(new BadCredentialsException("Invalid refresh token"));

        mockMvc.perform(post("/auth/refresh").with(csrf())
                        .cookie(new Cookie("refresh", "bogus")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revoke_clearsBothCookiesAndRevokesRefresh() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/refresh/revoke").with(csrf())
                        .cookie(new Cookie("refresh", "raw-refresh")))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(refreshTokenService).revoke("raw-refresh");
        assertThat(setCookie(result, "jwt")).contains("jwt=").contains("Max-Age=0");
        assertThat(setCookie(result, "refresh")).contains("refresh=").contains("Max-Age=0")
                .contains("Path=/auth/refresh");
    }

    @Test
    void revoke_withoutRefreshCookie_stillClearsCookies() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/refresh/revoke").with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(setCookie(result, "jwt")).contains("Max-Age=0");
        assertThat(setCookie(result, "refresh")).contains("Max-Age=0");
    }

    private String setCookie(MvcResult result, String cookieName) {
        return Optional.of(result.getResponse().getHeaders("Set-Cookie"))
                .flatMap(headers -> Arrays.stream(headers.toArray(new String[0]))
                        .filter(h -> h.startsWith(cookieName + "="))
                        .findFirst())
                .orElseThrow(() -> new AssertionError("Missing Set-Cookie for " + cookieName));
    }
}