package com.company.platform.commons.dto;

import com.company.platform.commons.enums.NotificationCategory;
import com.company.platform.commons.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record NotificationEventDto(UUID userId, NotificationType type,
                                   NotificationCategory category, @NotBlank String title,
                                   @NotBlank String message, String actionUrl) {
}
