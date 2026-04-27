package com.example.artbridgebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String medium;
    private String style;
    private Short creationYear;
    private Long viewsCount;
    private Long likesCount;
    private Instant createdAt;
    private Instant updatedAt;
}
