package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.entity.RefreshToken;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.repository.RefreshTokenRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenService {
    public record IssuedRefreshToken(String rawValue, Instant expiresAt) {
    }

    public record RotationResult(User user, IssuedRefreshToken newToken) {
    }

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties jwtProperties) {
        this.repository = repository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return createAndSave(user);
    }

    @Transactional
    public RotationResult rotate(String rawValue) {
        String hash = sha256Hex(rawValue);
        RefreshToken existing = repository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        Instant now = Instant.now();
        if (existing.getRevokedAt() != null || existing.getExpiresAt().isBefore(now)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        existing.setRevokedAt(now);
        existing.setLastUsedAt(now);
        repository.save(existing);

        User user = existing.getUser();
        IssuedRefreshToken newToken = createAndSave(user);
        return new RotationResult(user, newToken);
    }

    private IssuedRefreshToken createAndSave(User user) {
        String raw = generateRawValue();
        String hash = sha256Hex(raw);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.refreshTokenTtl());

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash);
        entity.setExpiresAt(expiresAt);
        repository.save(entity);

        return new IssuedRefreshToken(raw, expiresAt);
    }

    @Transactional
    public void revoke(String rawValue) {
        String hash = sha256Hex(rawValue);
        Optional<RefreshToken> maybe = repository.findByTokenHash(hash);
        if (maybe.isEmpty()) {
            return;
        }
        RefreshToken entity = maybe.get();
        if (entity.getRevokedAt() != null) {
            return;
        }
        entity.setRevokedAt(Instant.now());
        repository.save(entity);
    }

    private String generateRawValue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}