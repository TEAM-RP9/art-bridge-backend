package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.Dto.RegistrationRequestDto;
import com.example.artbridgebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/user")
    @Operation(summary = "Add new user", description = "Google Id, unique email and username(max 50 chars) must be provided")
    public void addNewUser(@Valid @RequestBody RegistrationRequestDto registrationRequestDto) throws Exception {
        userService.addNewUser(registrationRequestDto);
    }
}