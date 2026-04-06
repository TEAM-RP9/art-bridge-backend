package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/user")
    @Operation(summary = "Add new user", description = "Google Id and unique email must be provided")
    public ResponseEntity<Void> addNewUser(@Valid @RequestBody RegistrationRequest registrationRequest) {
        userService.addNewUser(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}