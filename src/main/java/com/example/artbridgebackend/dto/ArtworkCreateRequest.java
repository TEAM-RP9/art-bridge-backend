package com.example.artbridgebackend.dto;

import com.example.artbridgebackend.enums.ArtworkCategory;
import com.example.artbridgebackend.enums.ArtworkStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArtworkCreateRequest {

    @NotBlank
    @Size(min = 1, max = 120)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Image is required")
    private Long mediaId;

    private ArtworkCategory category;

    private String medium;

    private String style;

    @DecimalMin("0.01")
    @DecimalMax("10000.00")
    private BigDecimal width;

    @DecimalMin("0.01")
    @DecimalMax("10000.00")
    private BigDecimal height;

    private String dimensionUnit;

    @Min(1000)
    private Short creationYear;

    @Size(max = 10)
    private List<@NotBlank @Size(max = 30) String> tags;

    private ArtworkStatus status;

    private Boolean showOnProfile;

    @AssertTrue(message = "Description is required for published artworks")
    private boolean isDescriptionValidForStatus() {
        if (status != ArtworkStatus.PUBLISHED) return true;
        return description != null && !description.isBlank();
    }

    @AssertTrue(message = "Category is required for published artworks")
    private boolean isCategoryValidForStatus() {
        if (status != ArtworkStatus.PUBLISHED) return true;
        return category != null;
    }

    @AssertTrue(message = "Medium is required for published artworks")
    private boolean isMediumValidForStatus() {
        if (status != ArtworkStatus.PUBLISHED) return true;
        return medium != null && !medium.isBlank();
    }

    @AssertTrue(message = "Creation year is required for published artworks")
    private boolean isCreationYearValidForStatus() {
        if (status != ArtworkStatus.PUBLISHED) return true;
        return creationYear != null;
    }

    @AssertTrue(message = "Creation year cannot be more than one year in the future")
    private boolean isCreationYearInRange() {
        if (creationYear == null) return true;
        return creationYear <= Year.now().getValue() + 1;
    }
}