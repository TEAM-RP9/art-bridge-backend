package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.ArtworkCreateRequest;
import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.dto.PagedResponse;
import com.example.artbridgebackend.entity.Artwork;
import com.example.artbridgebackend.entity.Media;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.enums.ArtworkStatus;
import com.example.artbridgebackend.mapper.ArtworkMapper;
import com.example.artbridgebackend.repository.ArtworkRepository;
import com.example.artbridgebackend.repository.MediaRepository;
import com.example.artbridgebackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkMapper artworkMapper;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public PagedResponse<ArtworkResponse> getMyArtworks(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Artwork> artworkPage = artworkRepository.findAllByUserId(userId, pageable);

        List<ArtworkResponse> items = artworkPage.getContent().stream()
                .map(artwork -> artworkMapper.toResponse(artwork, storageService))
                .collect(Collectors.toList());

        return PagedResponse.<ArtworkResponse>builder()
                .items(items)
                .totalCount(artworkPage.getTotalElements())
                .currentPage(artworkPage.getNumber())
                .pageSize(artworkPage.getSize())
                .totalPages(artworkPage.getTotalPages())
                .build();
    }

    @Transactional
    public ArtworkResponse createArtwork(Long artistId, ArtworkCreateRequest request) {
        Media media = mediaRepository.findById(request.getMediaId())
                .orElseThrow(EntityNotFoundException::new);

        if (!media.getOwnerUser().getId().equals(artistId)) {
            throw new AccessDeniedException("Media does not belong to the requesting user");
        }

        if (request.getCreationYear() != null && request.getCreationYear() > Year.now().getValue() + 1) {
            throw new IllegalArgumentException("Creation year cannot be more than one year in the future");
        }

        User artist = userRepository.getReferenceById(artistId);

        ArtworkStatus status = request.getStatus() == null ? ArtworkStatus.DRAFT : request.getStatus();
        boolean showOnProfile;
        if (status == ArtworkStatus.DRAFT) {
            showOnProfile = false;
        } else {
            showOnProfile = request.getShowOnProfile() == null || request.getShowOnProfile();
        }

        Artwork artwork = buildArtwork(request, artist, status, showOnProfile, media);

        Artwork saved = artworkRepository.save(artwork);
        return artworkMapper.toResponse(saved, storageService);
    }

    private @NonNull Artwork buildArtwork(ArtworkCreateRequest request, User artist,
                                          ArtworkStatus status, boolean showOnProfile, Media media) {
        Artwork artwork = new Artwork();
        artwork.setUser(artist);
        artwork.setTitle(request.getTitle());
        artwork.setDescription(request.getDescription());
        artwork.setCategory(request.getCategory());
        artwork.setMedium(request.getMedium());
        artwork.setStyle(request.getStyle());
        artwork.setWidth(request.getWidth());
        artwork.setHeight(request.getHeight());
        artwork.setDimensionUnit(request.getDimensionUnit());
        artwork.setCreationYear(request.getCreationYear());
        artwork.setTags(normalizeTags(request.getTags()));
        artwork.setStatus(status);
        artwork.setShowOnProfile(showOnProfile);
        artwork.setMedia(media);
        return artwork;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }
}