package com.company.platform.file.repository;

import com.company.platform.file.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndOriginalName(UUID userId, String originalName);

    long deleteByUserIdAndOriginalName(UUID userId, String originalName);
}
