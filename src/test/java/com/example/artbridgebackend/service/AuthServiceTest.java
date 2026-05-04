package com.example.artbridgebackend.service;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.dto.GoogleLoginRequest;
import com.example.artbridgebackend.dto.LoginRequest;
import com.example.artbridgebackend.entity.Role;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.mapper.UserMapper;
import com.example.artbridgebackend.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String SECRET = "test-secret-value-of-exactly-32bytes!";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            SECRET, ACCESS_TTL, Duration.ofDays(30), "jwt", "refresh", false);

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private UserMapper userMapper;
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    private SecretKey jwtSecretKey;
    private AuthService authService;
    private UserService userService;

    private User seededUser;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userRepository = mock(UserRepository.class);
        userService = mock(UserService.class);
        userMapper = mock(UserMapper.class);
        googleIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        jwtSecretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        authService = new AuthService(authenticationManager, userRepository, userMapper,
                googleIdTokenVerifier, jwtSecretKey, JWT_PROPERTIES, userService);

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        seededUser = new User();
        seededUser.setId(42L);
        seededUser.setEmail("alice@example.com");
        seededUser.setPasswordHash("hashed");
        seededUser.setRole(role);
    }

    @Test
    void login_happyPath_returnsUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "alice@example.com", "secret123", java.util.List.of());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(seededUser));

        User user = authService.login(request);

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void generateToken_embedsUserIdEmailAndRoleWithTtl() {
        Instant before = Instant.now();
        String token = authService.generateToken(seededUser);
        Instant after = Instant.now();

        Claims claims = parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("alice@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getId()).isNotNull();
        assertThat(UUID.fromString(claims.getId())).isNotNull();

        Instant issuedAt = claims.getIssuedAt().toInstant();
        assertThat(issuedAt).isBetween(before.minusSeconds(5), after.plusSeconds(5));
        Instant expectedExp = issuedAt.plus(ACCESS_TTL);
        assertThat(claims.getExpiration().toInstant()).isBetween(
                expectedExp.minusSeconds(5), expectedExp.plusSeconds(5));
    }

    @Test
    void googleLogin_happyPath_returnsUser() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getSubject()).thenReturn("google-sub-123");
        when(payload.getEmail()).thenReturn("alice@example.com");
        when(payload.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(idToken);

        seededUser.setGoogleId("google-sub-123");
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.of(seededUser));

        User user = authService.googleLogin(request);

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void googleLogin_withNoExistingAccount_createsGoogleUser_viaUserService() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getSubject()).thenReturn("google-sub-456");
        when(payload.getEmail()).thenReturn("new@example.com");
        when(payload.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(idToken);

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        User createdUser = new User();
        createdUser.setId(84L);
        createdUser.setEmail("new@example.com");
        createdUser.setGoogleId("google-sub-456");
        createdUser.setRole(role);

        when(userRepository.findByGoogleId("google-sub-456")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createGoogleUser("new@example.com", "google-sub-456", null)).thenReturn(createdUser);

        User user = authService.googleLogin(request);

        assertThat(user).isSameAs(createdUser);
        verify(userService).createGoogleUser("new@example.com", "google-sub-456", null);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleLogin_brandNewUser_withArtistRole_createsArtist() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-google-token");
        request.setRole(com.example.artbridgebackend.dto.RegisterableRole.ARTIST);

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getSubject()).thenReturn("google-sub-999");
        when(payload.getEmail()).thenReturn("artist@example.com");
        when(payload.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(idToken);

        Role artistRole = new Role();
        artistRole.setId(2L);
        artistRole.setName("ARTIST");

        User createdUser = new User();
        createdUser.setId(99L);
        createdUser.setEmail("artist@example.com");
        createdUser.setGoogleId("google-sub-999");
        createdUser.setRole(artistRole);

        when(userRepository.findByGoogleId("google-sub-999")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("artist@example.com")).thenReturn(Optional.empty());
        when(userService.createGoogleUser("artist@example.com", "google-sub-999",
                com.example.artbridgebackend.dto.RegisterableRole.ARTIST)).thenReturn(createdUser);

        User user = authService.googleLogin(request);

        assertThat(user.getRole().getName()).isEqualTo("ARTIST");
        verify(userService).createGoogleUser("artist@example.com", "google-sub-999",
                com.example.artbridgebackend.dto.RegisterableRole.ARTIST);
    }

    @Test
    void googleLogin_existingUser_withArtistRole_doesNotChangeRole() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-google-token");
        request.setRole(com.example.artbridgebackend.dto.RegisterableRole.ARTIST);

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getSubject()).thenReturn("google-sub-123");
        when(payload.getEmail()).thenReturn("alice@example.com");
        when(payload.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(idToken);

        seededUser.setGoogleId("google-sub-123");
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.of(seededUser));

        User user = authService.googleLogin(request);

        assertThat(user.getRole().getName()).isEqualTo("USER");
        verify(userService, never()).createGoogleUser(anyString(), anyString(), any());
    }

    @Test
    void googleLogin_withExistingEmail_keepsCurrentLinkingPath() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getSubject()).thenReturn("google-sub-789");
        when(payload.getEmail()).thenReturn("alice@example.com");
        when(payload.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(idToken);

        when(userRepository.findByGoogleId("google-sub-789")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(seededUser));
        when(userRepository.save(seededUser)).thenReturn(seededUser);

        User user = authService.googleLogin(request);

        assertThat(user).isSameAs(seededUser);
        assertThat(user.getGoogleId()).isEqualTo("google-sub-789");
        verify(userRepository).save(seededUser);
        verify(userService, never()).createGoogleUser(anyString(), anyString());
    }

    @Test
    void parseToken_withValidToken_returnsClaims() {
        String token = authService.generateToken(seededUser);
        Claims claims = authService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("email", String.class)).isEqualTo("alice@example.com");
    }

    @Test
    void parseToken_withExpiredToken_throwsExpiredJwtException() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        String expiredToken = Jwts.builder()
                .subject("42")
                .claim("role", "USER")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(past.minus(1, ChronoUnit.HOURS)))
                .expiration(Date.from(past))
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> authService.parseToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseToken_withTamperedToken_throwsJwtException() {
        String token = authService.generateToken(seededUser);
        String tamperedToken = token + "tampered";

        assertThatThrownBy(() -> authService.parseToken(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseToken_withWrongKey_throwsJwtException() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key!!".getBytes(StandardCharsets.UTF_8));
        String tokenWithWrongKey = Jwts.builder()
                .subject("42")
                .claim("role", "USER")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> authService.parseToken(tokenWithWrongKey))
                .isInstanceOf(JwtException.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
