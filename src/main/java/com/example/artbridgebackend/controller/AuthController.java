package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.dto.AuthResponse;
import com.example.artbridgebackend.dto.GoogleLoginRequest;
import com.example.artbridgebackend.dto.LoginRequest;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
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
        addAccessCookie(httpResponse, authService.generateToken(user));
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
        addAccessCookie(httpResponse, authService.generateToken(user));
        return ResponseEntity.ok(toAuthResponse(user));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().getName());
    }

    private void addAccessCookie(HttpServletResponse httpResponse, String token) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.accessCookieName(), token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(jwtProperties.accessTokenTtl())
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}