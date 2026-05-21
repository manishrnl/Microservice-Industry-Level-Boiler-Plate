package com.company.platform.notification;

import com.company.platform.commons.dto.NotificationDto;
import com.company.platform.commons.enums.NotificationCategory;
import com.company.platform.commons.exception.ApiExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class NotificationService {
    private static final DateTimeFormatter LOGIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final NotificationRepository repository;

    @Value("${app.display-time-zone:Asia/Kolkata}")
    private String defaultTimeZone;

    @Transactional(readOnly = true)
    List<NotificationDto> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    NotificationDto markRead(UUID userId, UUID id) {
        Notification notification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        return toDto(notification);
    }

    @Transactional
    void markAllRead(UUID userId) {
        repository.markAllReadByUserId(userId);
    }

    @Transactional
    void delete(UUID userId, UUID id) {
        int deleted = repository.deleteByIdAndUserId(id, userId);
        if (deleted == 0) {
            throw new ApiExceptions.ResourceNotFoundException("Notification not found");
        }
    }

    @Transactional
    void deleteAll(UUID userId) {
        repository.deleteByUserId(userId);
    }

    @Transactional
    List<NotificationDto> recordLogin(LoginNotificationRequest request) {
        Notification login = create(
                request.userId(),
                NotificationCategory.AUTH,
                "New login detected",
                loginMessage(request),
                "/app/sessions"
        );
        seedAccountNotification(
                request.userId(),
                "Review your active sessions",
                "Keep your account secure by reviewing active sessions and revoking devices you do not recognize.",
                "/app/sessions"
        );
        seedAccountNotification(
                request.userId(),
                "Keep your profile up to date",
                "Add your latest name and avatar so your account stays recognizable across the platform.",
                "/app/profile"
        );
        return List.of(toDto(login));
    }

    private Notification create(UUID userId, NotificationCategory category, String title, String message, String actionUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setCategory(category);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setRead(false);
        return repository.save(notification);
    }

    private void seedAccountNotification(UUID userId, String title, String message, String actionUrl) {
        if (!repository.existsByUserIdAndTitle(userId, title)) {
            create(userId, NotificationCategory.SYSTEM, title, message, actionUrl);
        }
    }

    private String loginMessage(LoginNotificationRequest request) {
        String ipAddress = request.ipAddress() == null || request.ipAddress().isBlank()
                ? "an unknown IP"
                : request.ipAddress();
        String device = request.userAgent() == null || request.userAgent().isBlank()
                ? "an unknown device"
                : request.userAgent();
        String when = formatLoginTime(request);
        return "A new sign-in to your account was detected from " + ipAddress + " at " + when + ". Device: " + device;
    }

    private String formatLoginTime(LoginNotificationRequest request) {
        if (request.localTime() != null && !request.localTime().isBlank()) {
            return request.localTime() + formatLocationSuffix(request.timeZone());
        }
        OffsetDateTime loginAt = request.loginAt() == null ? OffsetDateTime.now() : request.loginAt();
        return loginAt.atZoneSameInstant(resolveZone(request.timeZone())).format(LOGIN_TIME_FORMATTER);
    }

    private String formatLocationSuffix(String timeZone) {
        return timeZone == null || timeZone.isBlank() ? "" : " (" + timeZone + ")";
    }

    private ZoneId resolveZone(String requestedZone) {
        String zone = requestedZone == null || requestedZone.isBlank() ? defaultTimeZone : requestedZone;
        try {
            return ZoneId.of(zone);
        } catch (RuntimeException ex) {
            return ZoneId.of(defaultTimeZone);
        }
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

}
