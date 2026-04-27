package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.dto.PagedResponse;
import com.example.artbridgebackend.entity.Artwork;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.mapper.ArtworkMapper;
import com.example.artbridgebackend.repository.ArtworkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {

    @Mock
    private ArtworkRepository artworkRepository;

    @Mock
    private ArtworkMapper artworkMapper;

    @InjectMocks
    private ArtworkService artworkService;

    private User user;
    private Artwork artwork;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("artist@example.com");

        artwork = new Artwork();
        artwork.setId(10L);
        artwork.setTitle("Starry Night");
        artwork.setUser(user);
        artwork.setUpdatedAt(Instant.now());
    }

    @Test
    void getMyArtworks_returnsPagedResponse() {
        Page<Artwork> page = new PageImpl<>(List.of(artwork));
        ArtworkResponse responseDto = ArtworkResponse.builder()
                .id(10L)
                .title("Starry Night")
                .build();

        when(artworkRepository.findAllByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(artworkMapper.toResponse(artwork)).thenReturn(responseDto);

        PagedResponse<ArtworkResponse> result = artworkService.getMyArtworks(1L, 0, 10);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getTitle()).isEqualTo("Starry Night");
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(1); // PageImpl reports actual content size if not provided otherwise
        assertThat(result.getTotalPages()).isEqualTo(1);
    }
}
