package com.example.artbridgebackend.mapper;

import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.dto.UserResponse;
import com.example.artbridgebackend.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(RegistrationRequest registrationRequest);

    UserResponse toUserResponse(User user);
}
