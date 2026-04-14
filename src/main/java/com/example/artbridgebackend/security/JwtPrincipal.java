package com.example.artbridgebackend.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtPrincipal {
    private Long id;
    private String email;
    private String role;
}