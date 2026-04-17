package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.dto.GoogleLoginRequest;
import com.example.artbridgebackend.dto.LoginRequest;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.enums.AccountStatus;
import com.example.artbridgebackend.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final SecretKey jwtSecretKey;
    private final JwtProperties jwtProperties;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       GoogleIdTokenVerifier googleIdTokenVerifier, SecretKey jwtSecretKey,
                       JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.jwtSecretKey = jwtSecretKey;
        this.jwtProperties = jwtProperties;
    }

    public User login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        checkAccountStatus(user);
        log.info("Login successful: userId={}", user.getId());
        return user;
        return AuthResponse.builder()
                .accessToken("placeholder")
                .tokenType("Bearer")
                .userId(user.getId())
                .build();
    }

    public User googleLogin(GoogleLoginRequest request) {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(request.getIdToken());
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Google token verification error: {}", e.getMessage());
            throw new BadCredentialsException("Authentication failed");
        }

        if (idToken == null) {
            log.warn("Google token verification failed");
            throw new BadCredentialsException("Authentication failed");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();

        if (!Boolean.TRUE.equals(emailVerified)) {
            log.warn("Login rejected: email not verified, googleId={}", googleId);
            throw new BadCredentialsException("Authentication failed");
        }

        Optional<User> userByGoogleId = userRepository.findByGoogleId(googleId);
        User user;

        if (userByGoogleId.isPresent()) {
            user = userByGoogleId.get();
            checkAccountStatus(user);
        } else {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isEmpty()) {
                log.warn("No account found for Google user, email={}", email);
                throw new BadCredentialsException("Authentication failed");
            }
            user = userByEmail.get();
            checkAccountStatus(user);
            user.setGoogleId(googleId);
            userRepository.save(user);
            log.info("Linked Google account to existing user, userId={}, googleId={}", user.getId(), googleId);
        }

        log.info("Successful Google login, userId={}", user.getId());
        return user;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void checkAccountStatus(User user) {
        if (user.getStatus() == AccountStatus.ACTIVE) {
            return;
        }

        switch (user.getStatus()) {
            case INACTIVE -> {
                log.info("Login rejected: account inactive, userId={}", user.getId());
                throw new DisabledException("Account is inactive");
            }
            case LOCKED -> {
                log.info("Login rejected: account locked, userId={}", user.getId());
                throw new LockedException("Account is locked");
            }
            default -> throw new AuthenticationServiceException("Unknown account status: " + user.getStatus());
        }
    }
}

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.accessTokenTtl());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getName())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }
}