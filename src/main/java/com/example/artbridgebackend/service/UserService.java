package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.RegistrationRequestDto;
import com.example.artbridgebackend.persistence.user.User;
import com.example.artbridgebackend.persistence.user.UserMapper;
import com.example.artbridgebackend.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public void addNewUser(RegistrationRequestDto registrationRequestDto) {
        createAndSaveUser(registrationRequestDto);
    }

    private void createAndSaveUser(RegistrationRequestDto registrationRequestDto) {
        User user = createNewUser(registrationRequestDto);
        userRepository.save(user);
    }

    private User createNewUser(RegistrationRequestDto registrationRequestDto) {
        return userMapper.toUser(registrationRequestDto);
    }
}
