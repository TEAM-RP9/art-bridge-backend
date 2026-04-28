package com.example.artbridgebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3Properties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String publicBaseUrl
) {}
