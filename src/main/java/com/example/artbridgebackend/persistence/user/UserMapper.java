package com.example.artbridgebackend.persistence.user;

import com.example.artbridgebackend.dto.RegistrationRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "googleId", target = "googleId")
    @Mapping(source = "email", target = "email")
    User toUser(RegistrationRequestDto registrationRequestDto);
}
