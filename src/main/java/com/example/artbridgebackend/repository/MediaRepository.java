package com.example.artbridgebackend.repository;

import com.example.artbridgebackend.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
}