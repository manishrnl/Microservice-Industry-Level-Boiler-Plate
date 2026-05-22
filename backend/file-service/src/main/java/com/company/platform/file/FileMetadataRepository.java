package com.company.platform.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndOriginalName(UUID userId, String originalName);

    long deleteByUserIdAndOriginalName(UUID userId, String originalName);
}
