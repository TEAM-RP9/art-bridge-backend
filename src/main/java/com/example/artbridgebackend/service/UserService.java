package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.entity.Role;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.mapper.UserMapper;
import com.example.artbridgebackend.repository.RoleRepository;
import com.example.artbridgebackend.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String DEFAULT_ROLE_NAME = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("Login failed: no account found");
                    return new UsernameNotFoundException("User not found");
                });

        if (user.getPasswordHash() == null) {
            log.info("Login failed: account has no password");
            throw new UsernameNotFoundException("User not found");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()))
        );
    }

    public User addNewUser(RegistrationRequest registrationRequest) {
        User user = userMapper.toUser(registrationRequest);
        if (user.getRole() == null) {
            user.setRole(resolveDefaultRole());
        }

        String encodedPassword = passwordEncoder.encode(registrationRequest.getPassword());
        user.setPasswordHash(encodedPassword);

        userRepository.save(user);
        return user;
    }

    public User createGoogleUser(String email, String googleId) {
        User user = new User();
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.setRole(resolveDefaultRole());
        user.setPasswordHash(passwordEncoder.encode(generateRandomPassword()));

        userRepository.save(user);
        return user;
    }

    private Role resolveDefaultRole() {
        return roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role '" + DEFAULT_ROLE_NAME + "' not found"));
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
