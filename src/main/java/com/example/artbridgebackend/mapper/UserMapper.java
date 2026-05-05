package com.example.artbridgebackend.mapper;

import com.example.artbridgebackend.dto.RegistrationRequest;
import com.example.artbridgebackend.dto.UserResponse;
import com.example.artbridgebackend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", ignore = true)
    User toUser(RegistrationRequest registrationRequest);

    @Mapping(target = "role", source = "role.name")
    UserResponse toUserResponse(User user);
}
