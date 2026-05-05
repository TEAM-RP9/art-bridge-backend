package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.entity.RefreshToken;
import com.example.artbridgebackend.entity.Role;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.repository.RefreshTokenRepository;
import com.example.artbridgebackend.service.RefreshTokenService.IssuedRefreshToken;
import com.example.artbridgebackend.service.RefreshTokenService.RotationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            "test-secret-value-of-exactly-32bytes!",
            Duration.ofMinutes(15),
            REFRESH_TTL,
            "jwt",
            "refresh",
            false
    );

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    private User seededUser;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, JWT_PROPERTIES);

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        seededUser = new User();
        seededUser.setId(42L);
        seededUser.setEmail("alice@example.com");
        seededUser.setRole(role);
    }

    @Test
    void issue_persistsHashedTokenWithConfiguredTtl() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        IssuedRefreshToken issued = service.issue(seededUser);
        Instant after = Instant.now();

        assertThat(issued.rawValue()).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(seededUser);
        assertThat(saved.getTokenHash()).isEqualTo(sha256Hex(issued.rawValue()));
        assertThat(saved.getRevokedAt()).isNull();
        assertThat(saved.getExpiresAt()).isBetween(
                before.plus(REFRESH_TTL).minusSeconds(5),
                after.plus(REFRESH_TTL).plusSeconds(5));
        assertThat(issued.expiresAt()).isEqualTo(saved.getExpiresAt());
    }

    @Test
    void rotate_revokesOldAndIssuesNew() {
        String rawOld = "raw-old";
        RefreshToken existing = new RefreshToken();
        existing.setUser(seededUser);
        existing.setTokenHash(sha256Hex(rawOld));
        existing.setExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));

        when(repository.findByTokenHash(sha256Hex(rawOld))).thenReturn(Optional.of(existing));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RotationResult result = service.rotate(rawOld);

        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(existing.getLastUsedAt()).isNotNull();
        assertThat(result.user()).isEqualTo(seededUser);
        assertThat(result.newToken().rawValue()).isNotBlank();
        assertThat(result.newToken().rawValue()).isNotEqualTo(rawOld);
    }

    @Test
    void rotate_rejectsRevokedToken() {
        String rawOld = "raw-old";
        RefreshToken existing = new RefreshToken();
        existing.setUser(seededUser);
        existing.setTokenHash(sha256Hex(rawOld));
        existing.setExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));
        existing.setRevokedAt(Instant.now().minusSeconds(60));

        when(repository.findByTokenHash(sha256Hex(rawOld))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.rotate(rawOld))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rotate_rejectsExpiredToken() {
        String rawOld = "raw-old";
        RefreshToken existing = new RefreshToken();
        existing.setUser(seededUser);
        existing.setTokenHash(sha256Hex(rawOld));
        existing.setExpiresAt(Instant.now().minusSeconds(60));

        when(repository.findByTokenHash(sha256Hex(rawOld))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.rotate(rawOld))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rotate_rejectsUnknownToken() {
        when(repository.findByTokenHash(sha256Hex("nope"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("nope"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void revoke_marksRowAsRevoked() {
        String raw = "raw";
        RefreshToken existing = new RefreshToken();
        existing.setUser(seededUser);
        existing.setTokenHash(sha256Hex(raw));
        existing.setExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));

        when(repository.findByTokenHash(sha256Hex(raw))).thenReturn(Optional.of(existing));

        service.revoke(raw);

        assertThat(existing.getRevokedAt()).isNotNull();
        verify(repository).save(existing);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}