package com.example.artbridgebackend.Service;

import com.example.artbridgebackend.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String DUMMY_HASH = "{NONE}no-password-set";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("Login failed: no account found for email={}", email);
                    return new UsernameNotFoundException("User not found");
                });

        String password = user.getPasswordHash() != null ? user.getPasswordHash() : DUMMY_HASH;

        if (user.getPasswordHash() == null) {
            log.info("Login failed: Google-only account attempted password login, email={}", email);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                Collections.emptyList()
        );
    }
}
