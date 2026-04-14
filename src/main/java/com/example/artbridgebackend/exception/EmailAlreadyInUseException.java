package com.example.artbridgebackend.exception;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException(String message) {
        super("Email Already In Use: " + message);
    }
}