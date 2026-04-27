package com.example.artbridgebackend.mapper;

import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.entity.Artwork;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtworkMapper {
    ArtworkResponse toResponse(Artwork artwork);
}
