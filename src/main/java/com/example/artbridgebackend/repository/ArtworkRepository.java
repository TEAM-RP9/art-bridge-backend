package com.example.artbridgebackend.repository;

import com.example.artbridgebackend.entity.Artwork;
import com.example.artbridgebackend.enums.ArtworkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    Page<Artwork> findAllByUserId(Long userId, Pageable pageable);
    Page<Artwork> findAllByStatusAndShowOnProfile(ArtworkStatus status, boolean showOnProfile, Pageable pageable);
}
