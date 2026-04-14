package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.*;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.exception.EmailAlreadyInUseException;
import com.example.artbridgebackend.mapper.UserMapper;
import com.example.artbridgebackend.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final UserMapper userMapper;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       GoogleIdTokenVerifier googleIdTokenVerifier,
                       UserMapper userMapper,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        log.info("Login successful: userId={}", user.getId());

        return AuthResponse.builder()
                .accessToken("placeholder")
                .tokenType("Bearer")
                .userId(user.getId())
                .build();
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
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
        } else {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isEmpty()) {
                log.warn("No account found for Google user, email={}", email);
                throw new BadCredentialsException("Authentication failed");
            }
            user = userByEmail.get();
            user.setGoogleId(googleId);
            userRepository.save(user);
            log.info("Linked Google account to existing user, userId={}, googleId={}", user.getId(), googleId);
        }

        log.info("Successful Google login, userId={}", user.getId());

        return AuthResponse.builder()
                .accessToken("placeholder")
                .tokenType("Bearer")
                .userId(user.getId())
                .build();
    }

    public UserResponse register(RegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use");
        }

        User savedUser = userService.addNewUser(request);
        return userMapper.toUserResponse(savedUser);
    }
}
