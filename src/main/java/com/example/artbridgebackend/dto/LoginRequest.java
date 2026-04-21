package com.example.artbridgebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @ToString.Exclude
    private String password;
}
