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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {

    @Mock
    private ArtworkRepository artworkRepository;

    @Mock
    private ArtworkMapper artworkMapper;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ArtworkService artworkService;

    private User artist;
    private Media media;
    private Artwork artwork;

    @BeforeEach
    void setUp() {
        artist = new User();
        artist.setId(1L);
        artist.setEmail("artist@example.com");

        media = new Media();
        media.setId(10L);
        media.setObjectKey("uuid.png");
        media.setOwnerUser(artist);

        artwork = new Artwork();
        artwork.setId(100L);
        artwork.setTitle("Starry Night");
        artwork.setUser(artist);
        artwork.setMedia(media);
        artwork.setUpdatedAt(Instant.now());
    }

    @Test
    void getMyArtworks_returnsPagedResponse() {
        Page<Artwork> page = new PageImpl<>(List.of(artwork));
        ArtworkResponse responseDto = ArtworkResponse.builder()
                .id(100L).title("Starry Night").build();

        when(artworkRepository.findAllByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(artworkMapper.toResponse(artwork, storageService)).thenReturn(responseDto);

        PagedResponse<ArtworkResponse> result = artworkService.getMyArtworks(1L, 0, 10);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getTitle()).isEqualTo("Starry Night");
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getPublicArtworks_returnsPagedResponse() {
        Page<Artwork> page = new PageImpl<>(List.of(artwork));
        ArtworkResponse responseDto = ArtworkResponse.builder()
                .id(100L).title("Starry Night").build();

        when(artworkRepository.findAllByStatusAndShowOnProfile(eq(ArtworkStatus.PUBLISHED), eq(true), any(Pageable.class))).thenReturn(page);
        when(artworkMapper.toResponse(artwork, storageService)).thenReturn(responseDto);

        PagedResponse<ArtworkResponse> result = artworkService.getPublicArtworks(0, 10);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getTitle()).isEqualTo("Starry Night");
        assertThat(result.getTotalCount()).isEqualTo(1);
        verify(artworkRepository).findAllByStatusAndShowOnProfile(eq(ArtworkStatus.PUBLISHED), eq(true), any(Pageable.class));
    }

    @Test
    void createArtwork_givenOwnedMedia_persistsAndReturnsResponse() {
        ArtworkCreateRequest request = draftRequest();
        ArtworkResponse expected = ArtworkResponse.builder().id(100L).title("WIP").build();

        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(userRepository.getReferenceById(1L)).thenReturn(artist);
        when(artworkRepository.save(any(Artwork.class))).thenReturn(artwork);
        when(artworkMapper.toResponse(any(Artwork.class), eq(storageService))).thenReturn(expected);

        ArtworkResponse result = artworkService.createArtwork(1L, request);

        assertThat(result).isEqualTo(expected);
        verify(artworkRepository).save(any(Artwork.class));
    }

    @Test
    void createArtwork_givenMissingMedia_throwsEntityNotFound() {
        ArtworkCreateRequest request = draftRequest();
        when(mediaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artworkService.createArtwork(1L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createArtwork_givenMediaOwnedByOtherUser_throwsAccessDenied() {
        User other = new User();
        other.setId(99L);
        media.setOwnerUser(other);

        ArtworkCreateRequest request = draftRequest();
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> artworkService.createArtwork(1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createArtwork_appliesServerDefaults() {
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(userRepository.getReferenceById(1L)).thenReturn(artist);
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artworkMapper.toResponse(any(Artwork.class), eq(storageService)))
                .thenAnswer(inv -> ArtworkResponse.builder().build());

        // null status -> DRAFT, showOnProfile forced false
        ArtworkCreateRequest req1 = draftRequest();
        req1.setStatus(null);
        req1.setShowOnProfile(true);
        artworkService.createArtwork(1L, req1);

        ArgumentCaptor<Artwork> captor = ArgumentCaptor.forClass(Artwork.class);
        verify(artworkRepository).save(captor.capture());
        Artwork saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ArtworkStatus.DRAFT);
        assertThat(saved.getShowOnProfile()).isFalse();
    }

    @Test
    void createArtwork_publishedWithNullShowOnProfile_defaultsToTrue() {
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(userRepository.getReferenceById(1L)).thenReturn(artist);
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artworkMapper.toResponse(any(Artwork.class), eq(storageService)))
                .thenAnswer(inv -> ArtworkResponse.builder().build());

        ArtworkCreateRequest req = publishedRequest();
        req.setShowOnProfile(null);
        artworkService.createArtwork(1L, req);

        ArgumentCaptor<Artwork> captor = ArgumentCaptor.forClass(Artwork.class);
        verify(artworkRepository).save(captor.capture());
        assertThat(captor.getValue().getShowOnProfile()).isTrue();
    }

    @Test
    void createArtwork_draftWithShowOnProfileTrue_forcedFalse() {
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(userRepository.getReferenceById(1L)).thenReturn(artist);
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artworkMapper.toResponse(any(Artwork.class), eq(storageService)))
                .thenAnswer(inv -> ArtworkResponse.builder().build());

        ArtworkCreateRequest req = draftRequest();
        req.setShowOnProfile(true);
        artworkService.createArtwork(1L, req);

        ArgumentCaptor<Artwork> captor = ArgumentCaptor.forClass(Artwork.class);
        verify(artworkRepository).save(captor.capture());
        assertThat(captor.getValue().getShowOnProfile()).isFalse();
    }

    @Test
    void createArtwork_normalizesTags() {
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(userRepository.getReferenceById(1L)).thenReturn(artist);
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artworkMapper.toResponse(any(Artwork.class), eq(storageService)))
                .thenAnswer(inv -> ArtworkResponse.builder().build());

        ArtworkCreateRequest req = draftRequest();
        req.setTags(List.of("  Oil  ", "CANVAS", "  "));
        artworkService.createArtwork(1L, req);

        ArgumentCaptor<Artwork> captor = ArgumentCaptor.forClass(Artwork.class);
        verify(artworkRepository).save(captor.capture());
        assertThat(captor.getValue().getTags()).containsExactly("oil", "canvas");
    }

    @Test
    void createArtwork_givenPublishedFutureYearBeyondNextYear_throwsValidation() {
        ArtworkCreateRequest req = draftRequest();
        req.setCreationYear((short) (Year.now().getValue() + 2));

        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> artworkService.createArtwork(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Creation year cannot be more than one year in the future");
    }

    private ArtworkCreateRequest draftRequest() {
        ArtworkCreateRequest req = new ArtworkCreateRequest();
        req.setTitle("WIP");
        req.setMediaId(10L);
        req.setStatus(ArtworkStatus.DRAFT);
        return req;
    }

    private ArtworkCreateRequest publishedRequest() {
        ArtworkCreateRequest req = new ArtworkCreateRequest();
        req.setTitle("Published Art");
        req.setMediaId(10L);
        req.setStatus(ArtworkStatus.PUBLISHED);
        req.setDescription("A masterpiece");
        req.setMedium("Oil");
        req.setStyle("Abstract");
        req.setCreationYear((short) 2020);
        return req;
    }
}