package com.example.artbridgebackend.Service;

import com.example.artbridgebackend.Dto.AuthResponse;
import com.example.artbridgebackend.Dto.LoginRequest;
import com.example.artbridgebackend.Entity.User;
import com.example.artbridgebackend.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        log.info("Login successful: userId={}, email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken("placeholder")
                .tokenType("Bearer")
                .userId(user.getId())
                .build();
    }
}
