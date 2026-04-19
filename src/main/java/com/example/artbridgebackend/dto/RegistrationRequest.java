package com.example.artbridgebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "Email is required!")
    @Email(message = "Must be a valid email address!")
    private String email;

    @NotBlank(message = "Password is required!")
    @Size(min = 15, message = "Password must be at least 15 characters")
    private String password;
}
