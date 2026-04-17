package com.example.artbridgebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ArtBridgeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtBridgeBackendApplication.class, args);
    }
}