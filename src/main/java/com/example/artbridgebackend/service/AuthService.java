package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.dto.GoogleLoginRequest;
import com.example.artbridgebackend.dto.LoginRequest;
import com.example.artbridgebackend.dto.RegisterableRole;
import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.dto.UserResponse;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.enums.AccountStatus;
import com.example.artbridgebackend.exception.EmailAlreadyInUseException;
import com.example.artbridgebackend.mapper.UserMapper;
import com.example.artbridgebackend.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.*;
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
    private final UserMapper userMapper;
    private final UserService userService;

    public AuthService(
                AuthenticationManager authenticationManager,
                UserRepository userRepository,
                UserMapper userMapper,
                GoogleIdTokenVerifier googleIdTokenVerifier,
                SecretKey jwtSecretKey,
                JwtProperties jwtProperties,
                UserService userService) {
            this.authenticationManager = authenticationManager;
            this.userRepository = userRepository;
            this.userMapper = userMapper;
            this.googleIdTokenVerifier = googleIdTokenVerifier;
            this.jwtSecretKey = jwtSecretKey;
            this.jwtProperties = jwtProperties;
            this.userService = userService;
        }

        public User login (LoginRequest request){
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            checkAccountStatus(user);
            log.info("Login successful: userId={}", user.getId());
            return user;
        }

        public User googleLogin (GoogleLoginRequest request){
            GoogleIdToken.Payload payload = verifyGooglePayload(request.getIdToken());
            User user = resolveGoogleUser(payload.getSubject(), payload.getEmail(), request.getRole());
            log.info("Successful Google login, userId={}", user.getId());
            return user;
        }

        public Claims parseToken (String token){
            return Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }

        private void checkAccountStatus (User user){
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

        private GoogleIdToken.Payload verifyGooglePayload(String rawIdToken) {
            GoogleIdToken idToken;
            try {
                idToken = googleIdTokenVerifier.verify(rawIdToken);
            } catch (GeneralSecurityException | IOException e) {
                log.warn("Google token verification error: {}", e.getMessage());
                throw new BadCredentialsException("Authentication failed");
            }

            if (idToken == null) {
                log.warn("Google token verification failed");
                throw new BadCredentialsException("Authentication failed");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                log.warn("Login rejected: email not verified, googleId={}", payload.getSubject());
                throw new BadCredentialsException("Authentication failed");
            }

            return payload;
        }

        private User resolveGoogleUser(String googleId, String email, RegisterableRole requestedRole) {
            Optional<User> userByGoogleId = userRepository.findByGoogleId(googleId);
            if (userByGoogleId.isPresent()) {
                User user = userByGoogleId.get();
                checkAccountStatus(user);
                return user;
            }

            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isEmpty()) {
                User user = userService.createGoogleUser(email, googleId, requestedRole);
                log.info("Created Google-backed user, userId={}, googleId={}", user.getId(), googleId);
                return user;
            }

            return linkGoogleAccount(userByEmail.get(), googleId);
        }

        private User linkGoogleAccount(User user, String googleId) {
            checkAccountStatus(user);
            user.setGoogleId(googleId);
            userRepository.save(user);
            log.info("Linked Google account to existing user, userId={}, googleId={}", user.getId(), googleId);
            return user;
        }

        public String generateToken (User user){
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

        public UserResponse register (RegistrationRequest request){
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new EmailAlreadyInUseException("Email already in use");
            }

            User savedUser = userService.addNewUser(request);
            return userMapper.toUserResponse(savedUser);
        }
}
