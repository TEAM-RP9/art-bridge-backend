package com.example.artbridgebackend.dto;

import com.example.artbridgebackend.enums.ArtworkCategory;
import com.example.artbridgebackend.enums.ArtworkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
    private ArtworkCategory category;
    private BigDecimal width;
    private BigDecimal height;
    private List<String> tags;
    private ArtworkStatus status;
    private Boolean showOnProfile;
    private Long mediaId;
}