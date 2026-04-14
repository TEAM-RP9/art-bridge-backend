package com.example.artbridgebackend.config;

import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            "test-secret-value-of-exactly-32bytes!",
            Duration.ofMinutes(15),
            Duration.ofDays(30),
            "jwt",
            "refresh"
    );

    @Mock
    private AuthService authService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(authService, JWT_PROPERTIES);
    }

    @Test
    void doFilterInternal_withValidJwtCookie_setsSecurityContext() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("42");
        when(claims.get("email", String.class)).thenReturn("alice@example.com");
        when(claims.get("role", String.class)).thenReturn("USER");

        request.setCookies(new Cookie("jwt", "valid-token"));
        when(authService.parseToken("valid-token")).thenReturn(claims);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(JwtPrincipal.class);
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getEmail()).isEqualTo("alice@example.com");
        assertThat(principal.getRole()).isEqualTo("USER");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoCookies_doesNotSetSecurityContext() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withOtherCookieButNoJwtCookie_doesNotSetSecurityContext() throws ServletException, IOException {
        request.setCookies(new Cookie("session", "something"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withExpiredToken_doesNotSetSecurityContext() throws ServletException, IOException {
        request.setCookies(new Cookie("jwt", "expired-token"));
        when(authService.parseToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNonNumericSubject_doesNotSetSecurityContext() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("admin");

        request.setCookies(new Cookie("jwt", "weird-token"));
        when(authService.parseToken("weird-token")).thenReturn(claims);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}