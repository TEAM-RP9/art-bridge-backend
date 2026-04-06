package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.entity.User;
import com.example.artbridgebackend.mapper.UserMapper;
import com.example.artbridgebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public void addNewUser(RegistrationRequest registrationRequest) {
        createAndSaveUser(registrationRequest);
    }

    private void createAndSaveUser(RegistrationRequest registrationRequest) {
        User user = createNewUser(registrationRequest);
        userRepository.save(user);
    }

    private User createNewUser(RegistrationRequest registrationRequest) {
        return userMapper.toUser(registrationRequest);
    }
}
