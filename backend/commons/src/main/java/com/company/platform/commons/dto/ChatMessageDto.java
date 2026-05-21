package com.company.platform.commons.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageDto(UUID id, UUID sessionId, String role, String content,
                             Integer tokensUsed, String model, LocalDateTime createdAt) {
}
