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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String DEFAULT_ROLE_NAME = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
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
            Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                    .orElseThrow(() -> new IllegalStateException(
                            "Default role '" + DEFAULT_ROLE_NAME + "' not found"));
            user.setRole(defaultRole);
        }
        userRepository.save(user);
        return user;
    }
}
