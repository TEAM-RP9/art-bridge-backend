package com.example.artbridgebackend.service;

import com.example.artbridgebackend.Dto.RegistrationRequestDto;
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

    public void addNewUser(RegistrationRequestDto registrationRequestDto) throws Exception {
        validateUsernameIsAvailable(registrationRequestDto.getUsername());
        createAndSaveUser(registrationRequestDto);
    }

    private void validateUsernameIsAvailable(String username) throws Exception {
        boolean usernameExists = userRepository.usernameExistsBy(username);
        if (usernameExists) {
            throw new Exception("Username already in use!");
        }
    }

    private void createAndSaveUser(RegistrationRequestDto registrationRequestDto) {
        User user = createNewUser(registrationRequestDto);
        userRepository.save(user);
    }

    private User createNewUser(RegistrationRequestDto registrationRequestDto) {
        return userMapper.toUser(registrationRequestDto);
    }
}
