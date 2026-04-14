package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.dto.AuthResponse;
import com.example.artbridgebackend.dto.GoogleLoginRequest;
import com.example.artbridgebackend.dto.LoginRequest;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.service.AuthService;
import com.example.artbridgebackend.service.RefreshTokenService;
import com.example.artbridgebackend.service.RefreshTokenService.IssuedRefreshToken;
import com.example.artbridgebackend.service.RefreshTokenService.RotationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private static final String REFRESH_COOKIE_PATH = "/auth/refresh";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          JwtProperties jwtProperties) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    @Operation(
            summary = "User Login",
            description = "Authenticate a user using email/password credentials"
    )
    @ApiResponse(
            responseCode = "200",
            description = "User authenticated successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
    )
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request,
                                              HttpServletResponse httpResponse) {
        User user = authService.login(request);
        issueTokens(user, httpResponse);
        return ResponseEntity.ok(toAuthResponse(user));
    }

    @PostMapping("/oauth/google")
    @Operation(
            summary = "Google OAuth Login",
            description = "Authenticate a user using a Google ID token"
    )
    @ApiResponse(
            responseCode = "200",
            description = "User authenticated successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid or unverified Google token"
    )
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody @Valid GoogleLoginRequest request,
                                                    HttpServletResponse httpResponse) {
        User user = authService.googleLogin(request);
        issueTokens(user, httpResponse);
        return ResponseEntity.ok(toAuthResponse(user));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Rotate the refresh token and issue a new access token"
    )
    @ApiResponse(responseCode = "204", description = "New cookies issued")
    @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token")
    public ResponseEntity<Void> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawRefresh = extractRefreshCookie(httpRequest);
        if (rawRefresh == null) {
            throw new BadCredentialsException("Missing refresh token");
        }

        RotationResult rotation = refreshTokenService.rotate(rawRefresh);
        String accessToken = authService.generateToken(rotation.user());
        addAccessCookie(httpResponse, accessToken);
        addRefreshCookie(httpResponse, rotation.newToken().rawValue(), rotation.newToken().expiresAt());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh/revoke")
    @Operation(
            summary = "Logout",
            description = "Revoke the current refresh token and clear auth cookies"
    )
    @ApiResponse(responseCode = "204", description = "Cookies cleared")
    public ResponseEntity<Void> revoke(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawRefresh = extractRefreshCookie(httpRequest);
        if (rawRefresh != null) {
            refreshTokenService.revoke(rawRefresh);
        }
        clearAccessCookie(httpResponse);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private void issueTokens(User user, HttpServletResponse httpResponse) {
        String accessToken = authService.generateToken(user);
        IssuedRefreshToken refresh = refreshTokenService.issue(user);
        addAccessCookie(httpResponse, accessToken);
        addRefreshCookie(httpResponse, refresh.rawValue(), refresh.expiresAt());
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().getName());
    }

    private String extractRefreshCookie(HttpServletRequest httpRequest) {
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies == null) {
            return null;
        }
        String cookieName = jwtProperties.refreshCookieName();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addAccessCookie(HttpServletResponse httpResponse, String token) {
        writeCookie(httpResponse, jwtProperties.accessCookieName(), token, "/", jwtProperties.accessTokenTtl());
    }

    private void addRefreshCookie(HttpServletResponse httpResponse, String rawRefresh, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        if (maxAge.isNegative()) {
            maxAge = Duration.ZERO;
        }
        writeCookie(httpResponse, jwtProperties.refreshCookieName(), rawRefresh, REFRESH_COOKIE_PATH, maxAge);
    }

    private void clearAccessCookie(HttpServletResponse httpResponse) {
        writeCookie(httpResponse, jwtProperties.accessCookieName(), "", "/", Duration.ZERO);
    }

    private void clearRefreshCookie(HttpServletResponse httpResponse) {
        writeCookie(httpResponse, jwtProperties.refreshCookieName(), "", REFRESH_COOKIE_PATH, Duration.ZERO);
    }

    private void writeCookie(HttpServletResponse httpResponse, String name, String value,
                             String path, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}