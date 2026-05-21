package com.company.platform.commons.dto;

import com.company.platform.commons.enums.NotificationCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(UUID id, NotificationCategory category, String title,
                              String message, String actionUrl, boolean read,
                              LocalDateTime createdAt) {
}
