package com.example.artbridgebackend.config;

import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractJwtFromCookie(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = authService.parseToken(token);
            UsernamePasswordAuthenticationToken authentication = buildAuthentication(claims, request);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(Claims claims, HttpServletRequest request) {
        long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            log.warn("JWT sub is not numeric: {}", claims.getSubject());
            return null;
        }
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        JwtPrincipal principal = new JwtPrincipal(userId, email, role);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    private String extractJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        String cookieName = jwtProperties.accessCookieName();
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}