package com.example.artbridgebackend.persistence.user;

import com.example.artbridgebackend.dto.RegistrationRequestDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    User toUser(RegistrationRequestDto registrationRequestDto);
}
