package com.example.artbridgebackend.mapper;

import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.entity.Artwork;
import com.example.artbridgebackend.service.StorageService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ArtworkMapper {

    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "mediaId", ignore = true)
    ArtworkResponse toResponseBase(Artwork artwork);

    default ArtworkResponse toResponse(Artwork artwork, StorageService storageService) {
        ArtworkResponse response = toResponseBase(artwork);
        response.setImageUrl(storageService.buildPublicUrl(artwork.getMedia()));
        response.setMediaId(artwork.getMedia().getId());
        return response;
    }
}