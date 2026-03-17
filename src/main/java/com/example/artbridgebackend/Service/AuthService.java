package com.example.artbridgebackend.Service;

import com.example.artbridgebackend.Dto.AuthResponse;
import com.example.artbridgebackend.Dto.LoginRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public AuthResponse login(LoginRequest request) {
        return new AuthResponse();
    }
}
