package com.company.platform.commons.dto;

import com.company.platform.commons.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEventDto(UUID userId, String username, AuditAction action,
                            String resourceType, String resourceId, String ipAddress,
                            String userAgent, String traceId, Map<String, Object> beforeState,
                            Map<String, Object> afterState, String status, String errorMessage,
                            LocalDateTime createdAt) {
}
