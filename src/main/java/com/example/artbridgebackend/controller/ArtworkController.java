package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.dto.PagedResponse;
import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.ArtworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/artworks")
@RequiredArgsConstructor
@Validated
@Tag(name = "Artwork", description = "Artwork management APIs")
public class ArtworkController {

    private final ArtworkService artworkService;

    @GetMapping("/my")
    @Operation(
            summary = "Get signed-in artist's artworks",
            description = "Fetch a paginated list of artworks owned by the authenticated user, ordered by most recently updated."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved artworks"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
    )
    public ResponseEntity<PagedResponse<ArtworkResponse>> getMyArtworks(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        return ResponseEntity.ok(artworkService.getMyArtworks(principal.getId(), page, size));
    }
}
