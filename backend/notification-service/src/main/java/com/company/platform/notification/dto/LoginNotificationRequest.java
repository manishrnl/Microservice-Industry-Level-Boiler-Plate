package com.company.platform.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LoginNotificationRequest(UUID userId, String email, String name, String sessionId,
                                       String ipAddress, String userAgent, String timeZone, String localTime,
                                       OffsetDateTime loginAt) {
}
