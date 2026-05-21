package com.company.platform.commons.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatSessionDto(UUID id, UUID userId, String title, String modelUsed,
                             int totalTokens, boolean archived, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
}
