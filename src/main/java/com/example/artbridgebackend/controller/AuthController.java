package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.dto.AuthResponse;
import com.example.artbridgebackend.dto.GoogleLoginRequest;
import com.example.artbridgebackend.dto.LoginRequest;
import com.example.artbridgebackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "User Login",
            description = "Authenticate a user using email/password or OAuth token"
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
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }
}
