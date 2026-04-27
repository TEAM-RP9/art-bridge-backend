package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.config.SecurityConfig;
import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.dto.PagedResponse;
import com.example.artbridgebackend.security.JwtPrincipal;
import com.example.artbridgebackend.service.ArtworkService;
import com.example.artbridgebackend.service.AuthService;
import com.example.artbridgebackend.service.RefreshTokenService;
import com.example.artbridgebackend.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        JwtPrincipal principal = new JwtPrincipal(42L, "test@example.com", "USER");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        PagedResponse<ArtworkResponse> response = PagedResponse.<ArtworkResponse>builder()
                .items(List.of(ArtworkResponse.builder().id(1L).title("Artwork 1").build()))
                .totalCount(1)
                .currentPage(0)
                .pageSize(10)
                .totalPages(1)
                .build();

        when(artworkService.getMyArtworks(eq(42L), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/artworks/my")
                        .with(authentication(authentication))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Artwork 1"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void getMyArtworks_whenNotAuthenticated_returns403() throws Exception {
        mockMvc.perform(get("/artworks/my")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
