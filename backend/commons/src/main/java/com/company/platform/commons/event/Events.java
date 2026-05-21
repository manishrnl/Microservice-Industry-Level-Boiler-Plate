package com.company.platform.commons.event;

import com.company.platform.commons.enums.AuditAction;
import com.company.platform.commons.enums.EventStatus;
import com.company.platform.commons.enums.NotificationCategory;
import com.company.platform.commons.enums.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public final class Events {
    private Events() {
    }

    public record UserRegisteredEvent(UUID userId, String email, String name,
                                      LocalDateTime registeredAt) {
    }

    public record UserLoginEvent(UUID userId, String email, String sessionId, String ipAddress,
                                 LocalDateTime loginAt) {
    }

    public record NewDeviceLoginEvent(UUID userId, String email, String deviceId,
                                      String ipAddress, String userAgent) {
    }

    public record PasswordChangedEvent(UUID userId, String email, LocalDateTime changedAt) {
    }

    public record PaymentInitiatedEvent(UUID paymentId, UUID userId, BigDecimal amount,
                                        String currency) {
    }

    public record PaymentCompletedEvent(UUID paymentId, UUID userId, BigDecimal amount,
                                        String currency) {
    }

    public record PaymentFailedEvent(UUID paymentId, UUID userId, BigDecimal amount,
                                     String currency, String reason) {
    }

    public record AuditEvent(UUID userId, String username, AuditAction action,
                             String resourceType, String resourceId, String traceId,
                             Map<String, Object> beforeState, Map<String, Object> afterState,
                             EventStatus status) {
    }

    public record NotificationEvent(UUID userId, NotificationType type,
                                    NotificationCategory category, String title,
                                    String message, String actionUrl) {
    }
}
