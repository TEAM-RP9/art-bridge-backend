package com.example.artbridgebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "Google Id is required!")
    private String googleId;

    @NotBlank(message = "Email is required!")
    @Email(message = "Must be a valid email address!")
    private String email;
}
