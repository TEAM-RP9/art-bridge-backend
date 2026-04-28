package com.example.artbridgebackend.service;

import com.example.artbridgebackend.dto.ArtworkResponse;
import com.example.artbridgebackend.dto.PagedResponse;
import com.example.artbridgebackend.entity.Artwork;
import com.example.artbridgebackend.mapper.ArtworkMapper;
import com.example.artbridgebackend.repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkMapper artworkMapper;

    @Transactional(readOnly = true)
    public PagedResponse<ArtworkResponse> getMyArtworks(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Artwork> artworkPage = artworkRepository.findAllByUserId(userId, pageable);

        List<ArtworkResponse> items = artworkPage.getContent().stream()
                .map(artworkMapper::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ArtworkResponse>builder()
                .items(items)
                .totalCount(artworkPage.getTotalElements())
                .currentPage(artworkPage.getNumber())
                .pageSize(artworkPage.getSize())
                .totalPages(artworkPage.getTotalPages())
                .build();
    }
}
