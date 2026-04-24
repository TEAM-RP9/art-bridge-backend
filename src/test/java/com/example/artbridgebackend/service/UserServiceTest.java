package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.entity.Role;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.mapper.UserMapper;
import com.example.artbridgebackend.repository.RoleRepository;
import com.example.artbridgebackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    private RegistrationRequest registrationRequest;
    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        registrationRequest = new RegistrationRequest("test@example.com", "suvaline_parool_mis_vajab_hashimist123");
        user = new User();
        user.setEmail("test@example.com");
        role = new Role();
        role.setName("USER");
    }

    @Test
    void addNewUser_ShouldHashPasswordCorrectly() {
        PasswordEncoder realEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
        UserService userServiceWithRealEncoder = new UserService(userRepository, roleRepository, userMapper, realEncoder);

        when(userMapper.toUser(registrationRequest)).thenReturn(user);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User savedUser = userServiceWithRealEncoder.addNewUser(registrationRequest);

        assertNotNull(savedUser.getPasswordHash());
        assertTrue(realEncoder.matches(registrationRequest.getPassword(), savedUser.getPasswordHash()));
        verify(userRepository).save(user);
    }

    @Test
    void createGoogleUser_setsGoogleId_defaultRole_andHashedRandomPassword() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserService userService = new UserService(userRepository, roleRepository, userMapper, passwordEncoder);
        ArgumentCaptor<String> rawPasswordCaptor = ArgumentCaptor.forClass(String.class);

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.createGoogleUser("google@example.com", "google-id-123");

        verify(passwordEncoder).encode(rawPasswordCaptor.capture());
        assertEquals("google@example.com", savedUser.getEmail());
        assertEquals("google-id-123", savedUser.getGoogleId());
        assertSame(role, savedUser.getRole());
        assertEquals("encoded-random-password", savedUser.getPasswordHash());
        assertTrue(rawPasswordCaptor.getValue().length() >= 43);
        verify(userRepository).save(savedUser);
    }
}
