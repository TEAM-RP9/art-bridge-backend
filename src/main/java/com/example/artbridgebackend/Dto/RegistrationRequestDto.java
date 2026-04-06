package com.example.artbridgebackend.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequestDto {

    @NotBlank(message = "Google Id is required!")
    private String googleId;

    @NotBlank(message = "Email is required!")
    @Email(message = "Must be a valid email address!")
    private String email;

    @NotBlank(message = "Username is required!")
    @Size(max = 50, message = "Username must be less than 50 characters long!")
    private String username;
}
