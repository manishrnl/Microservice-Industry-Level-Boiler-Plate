package com.company.platform.commons.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileMetadataDto(UUID id, UUID userId, String originalName, String contentType,
                              long sizeBytes, boolean isPublic, LocalDateTime createdAt) {
}
