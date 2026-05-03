package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.dto.ArtworkCreateRequest;
import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.dto.PagedResponse;
import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.ArtworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/artworks")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ARTIST')")
@Tag(name = "Artwork", description = "Artwork management APIs")
public class ArtworkController {

    private final ArtworkService artworkService;

    @GetMapping("/my")
    @Operation(
            summary = "Get signed-in artist's artworks",
            description = "Fetch a paginated list of artworks owned by the authenticated user, ordered by most recently updated."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved artworks")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<PagedResponse<ArtworkResponse>> getMyArtworks(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "3") @Min(1) @Max(9) int size) {
        return ResponseEntity.ok(artworkService.getMyArtworks(principal.getId(), page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an artwork", description = "Creates a new artwork for the authenticated artist.")
    @ApiResponse(responseCode = "201", description = "Artwork created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Media not found")
    @ApiResponse(responseCode = "409", description = "Media already linked to another artwork")
    public ArtworkResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody @Valid ArtworkCreateRequest request) {
        return artworkService.createArtwork(principal.getId(), request);
    }
}