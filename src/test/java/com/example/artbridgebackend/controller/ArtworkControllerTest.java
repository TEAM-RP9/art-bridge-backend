package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.config.SecurityConfig;
import com.example.artbridgebackend.dto.ArtworkCreateRequest;
import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.dto.PagedResponse;
import com.example.artbridgebackend.enums.ArtworkCategory;
import com.example.artbridgebackend.enums.ArtworkStatus;
import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.ArtworkService;
import com.example.artbridgebackend.service.AuthService;
import com.example.artbridgebackend.service.RefreshTokenService;
import com.example.artbridgebackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtworkController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-value-of-exactly-32bytes!",
        "jwt.access-token-ttl=15m",
        "jwt.refresh-token-ttl=30d",
        "jwt.access-cookie-name=jwt",
        "jwt.refresh-cookie-name=refresh",
        "google.oauth.client-id=test-client-id"
})
class ArtworkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ArtworkService artworkService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Test
    void getMyArtworks_whenAuthenticated_returnsArtworks() throws Exception {
        Authentication auth = artistAuth(42L);

        PagedResponse<ArtworkResponse> response = PagedResponse.<ArtworkResponse>builder()
                .items(List.of(ArtworkResponse.builder().id(1L).title("Artwork 1")
                        .status(ArtworkStatus.DRAFT).showOnProfile(false).build()))
                .totalCount(1)
                .currentPage(0)
                .pageSize(10)
                .totalPages(1)
                .build();

        when(artworkService.getMyArtworks(eq(42L), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/artworks/my")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Artwork 1"))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void getMyArtworks_whenNotAuthenticated_returns401() throws Exception {
        mockMvc.perform(get("/artworks/my")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyArtworks_whenAuthenticatedAsNonArtist_returns403() throws Exception {
        mockMvc.perform(get("/artworks/my")
                        .with(authentication(userAuth(10L)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_givenValidPublishedRequest_returns201AndResponse() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("Starry Night");
        request.setDescription("A masterpiece");
        request.setMediaId(1L);
        request.setCategory(ArtworkCategory.PAINTING);
        request.setMedium("Oil");
        request.setCreationYear((short) 2020);
        request.setStatus(ArtworkStatus.PUBLISHED);
        request.setShowOnProfile(true);
        request.setTags(List.of("impressionism", "blue"));

        ArtworkResponse response = ArtworkResponse.builder()
                .id(100L).title("Starry Night").status(ArtworkStatus.PUBLISHED)
                .showOnProfile(true).imageUrl("http://cdn/img.png").mediaId(1L)
                .category(ArtworkCategory.PAINTING).tags(List.of("impressionism", "blue"))
                .build();

        when(artworkService.createArtwork(eq(42L), any(ArtworkCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.showOnProfile").value(true))
                .andExpect(jsonPath("$.imageUrl").value("http://cdn/img.png"))
                .andExpect(jsonPath("$.mediaId").value(1))
                .andExpect(jsonPath("$.category").value("PAINTING"))
                .andExpect(jsonPath("$.tags[0]").value("impressionism"));
    }

    @Test
    void create_givenValidDraftRequest_returns201AndForcesShowOnProfileFalse() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("WIP");
        request.setMediaId(2L);
        request.setStatus(ArtworkStatus.DRAFT);

        ArtworkResponse response = ArtworkResponse.builder()
                .id(101L).title("WIP").status(ArtworkStatus.DRAFT).showOnProfile(false).mediaId(2L)
                .build();

        when(artworkService.createArtwork(eq(42L), any(ArtworkCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.showOnProfile").value(false));
    }

    @Test
    void create_givenMissingTitle_returns400() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setMediaId(1L);

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").exists());
    }

    @Test
    void create_givenMissingMediaId_returns400() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='mediaId')]").exists());
    }

    @Test
    void create_givenPublishedMissingDescription_returns400() throws Exception {
        ArtworkCreateRequest request = publishedMinusField("description");

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void create_givenPublishedMissingCategory_returns400() throws Exception {
        ArtworkCreateRequest request = publishedMinusField("category");

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void create_givenPublishedMissingMedium_returns400() throws Exception {
        ArtworkCreateRequest request = publishedMinusField("medium");

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void create_givenPublishedMissingCreationYear_returns400() throws Exception {
        ArtworkCreateRequest request = publishedMinusField("creationYear");

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void create_givenTooManyTags_returns400() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(1L);
        request.setTags(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"));

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='tags')]").exists());
    }

    @Test
    void create_givenTagTooLong_returns400() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(1L);
        request.setTags(List.of("a".repeat(31)));

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void create_givenWidthOutOfRange_returns400() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(1L);
        request.setWidth(java.math.BigDecimal.valueOf(0));

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='width')]").exists());
    }

    @Test
    void create_givenUnauthenticated_returns401() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(1L);

        mockMvc.perform(post("/artworks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_givenAuthenticatedAsNonArtist_returns403() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(1L);

        mockMvc.perform(post("/artworks")
                        .with(authentication(userAuth(10L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_givenMediaOwnedByOtherUser_returns403() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(1L);

        when(artworkService.createArtwork(eq(42L), any(ArtworkCreateRequest.class)))
                .thenThrow(new AccessDeniedException("Media does not belong to the requesting user"));

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_givenMissingMedia_returns404() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(999L);

        when(artworkService.createArtwork(eq(42L), any(ArtworkCreateRequest.class)))
                .thenThrow(new EntityNotFoundException());

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_givenMediaAlreadyLinked_returns409() throws Exception {
        ArtworkCreateRequest request = new ArtworkCreateRequest();
        request.setTitle("My Art");
        request.setMediaId(1L);

        when(artworkService.createArtwork(eq(42L), any(ArtworkCreateRequest.class)))
                .thenThrow(new DataIntegrityViolationException("artwork_media_id_unique"));

        mockMvc.perform(post("/artworks")
                        .with(authentication(artistAuth(42L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    private Authentication artistAuth(Long userId) {
        JwtPrincipal principal = new JwtPrincipal(userId, "artist@example.com", "ARTIST");
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ARTIST")));
    }

    private Authentication userAuth(Long userId) {
        JwtPrincipal principal = new JwtPrincipal(userId, "user@example.com", "USER");
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private ArtworkCreateRequest publishedMinusField(String missingField) {
        ArtworkCreateRequest req = new ArtworkCreateRequest();
        req.setTitle("Art");
        req.setMediaId(1L);
        req.setStatus(ArtworkStatus.PUBLISHED);
        if (!"description".equals(missingField)) req.setDescription("A great piece");
        if (!"category".equals(missingField)) req.setCategory(ArtworkCategory.PAINTING);
        if (!"medium".equals(missingField)) req.setMedium("Oil");
        if (!"creationYear".equals(missingField)) req.setCreationYear((short) 2020);
        return req;
    }
}