package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.AuthResponse;
import com.example.artbridgebackend.dto.LoginRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public AuthResponse login(LoginRequest request) {
        return new AuthResponse();
    }
}
