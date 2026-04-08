package com.example.artbridgebackend.controller;

import com.example.artbridgebackend.config.SecurityConfig;
import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.repository.UserRepository;
import com.example.artbridgebackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void addNewUser_withValidRequest_returns201() throws Exception {
        RegistrationRequest request = new RegistrationRequest("google-id-123", "test@example.com");

        doNothing().when(userService).addNewUser(any(RegistrationRequest.class));

        mockMvc.perform(post("/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"googleId": "google-id-123",
                                "email": "test@example.com"}
                                """))
                .andExpect(status().isCreated());

        verify(userService).addNewUser(request);
    }

    @Test
    void addNewUser_withInvalidEmail_returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest("google-id-123", "invalid-email");

        mockMvc.perform(post("/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"googleId": "google-id-123",
                                "email": "invalid-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void addNewUser_withBlankGoogleId_returns400() throws Exception {
        RegistrationRequest request = new RegistrationRequest("", "test@example.com");

        mockMvc.perform(post("/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"googleId": "",
                                "email": "test@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("googleId"));
    }

    @Test
    void addNewUser_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
