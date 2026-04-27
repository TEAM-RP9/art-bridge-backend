package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.JwtProperties;
import com.example.artbridgebackend.config.SecurityConfig;
import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.repository.UserRepository;
import com.example.artbridgebackend.service.AuthService;
import com.example.artbridgebackend.service.RefreshTokenService;
import com.example.artbridgebackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-value-of-exactly-32bytes!",
        "jwt.access-token-ttl=15m",
        "jwt.refresh-token-ttl=30d",
        "jwt.access-cookie-name=jwt",
        "jwt.refresh-cookie-name=refresh"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void addNewUser_withValidRequest_returns201() throws Exception {
        RegistrationRequest request = new RegistrationRequest("test@example.com", "validPassword123");

        when(userService.addNewUser(any(RegistrationRequest.class))).thenReturn(new User());

        mockMvc.perform(post("/user/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com",
                                "password": "validPassword123"}
                                """))
                .andExpect(status().isCreated());

        verify(userService).addNewUser(request);
    }

    @Test
    void addNewUser_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/user/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "invalid-email",
                                "password": "validPassword123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='email')]").exists());
    }

    @Test
    void addNewUser_withBlankPassword_returns400() throws Exception {
        mockMvc.perform(post("/user/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test@example.com",
                                "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='password')]").exists());
    }

    @Test
    void addNewUser_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/user/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
