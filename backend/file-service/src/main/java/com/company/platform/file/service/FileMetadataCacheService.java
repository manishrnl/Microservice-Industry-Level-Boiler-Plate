package com.company.platform.file.service;

import com.company.platform.commons.dto.FileMetadataDto;
import com.company.platform.commons.exception.ApiExceptions;
import com.company.platform.file.model.FileMetadata;
import com.company.platform.file.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileMetadataCacheService {
    private final FileMetadataRepository repository;

    @Cacheable(cacheNames = "fileMetadata", key = "#id")
    public FileMetadataDto metadata(UUID id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("File not found"));
    }

    @Cacheable(cacheNames = "fileDownloadUrls", key = "#id + '|' + #downloadBaseUrl")
    public Map<String, String> downloadUrl(UUID id, String downloadBaseUrl) {
        FileMetadataDto metadata = metadata(id);
        return Map.of(
                "url", downloadUrlFor(id, downloadBaseUrl),
                "filename", metadata.originalName(),
                "contentType", metadata.contentType()
        );
    }

    @Cacheable(cacheNames = "userFiles", key = "#userId")
    public List<FileMetadataDto> userFiles(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @CacheEvict(cacheNames = {"fileMetadata", "fileDownloadUrls", "userFiles"}, allEntries = true)
    public void evictAll() {
    }

    private String downloadUrlFor(UUID id, String downloadBaseUrl) {
        String baseUrl = downloadBaseUrl == null || downloadBaseUrl.isBlank()
                ? "http://localhost:8080"
                : downloadBaseUrl.trim();
        return StringUtils.trimTrailingCharacter(baseUrl, '/') + "/api/v1/files/" + id + "/download";
    }

    private FileMetadataDto toDto(FileMetadata metadata) {
        return new FileMetadataDto(
                metadata.getId(),
                metadata.getUserId(),
                metadata.getOriginalName(),
                metadata.getContentType(),
                metadata.getSizeBytes(),
                metadata.isPublic(),
                metadata.getCreatedAt()
        );
    }
}
