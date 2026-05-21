package com.company.platform.notification;

import com.company.platform.commons.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
class NotificationController {
    private final NotificationService notifications;

    @GetMapping
    List<NotificationDto> list(@RequestHeader("X-User-Id") UUID userId) {
        return notifications.list(userId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@RequestHeader("X-User-Id") UUID userId) throws Exception {
        SseEmitter emitter = new SseEmitter(Duration.ofHours(6).toMillis());
        emitter.send(SseEmitter.event().comment("connected"));
        return emitter;
    }

    @PatchMapping("/{id}/read")
    NotificationDto markRead(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id) {
        return notifications.markRead(userId, id);
    }

    @PatchMapping("/read-all")
    List<NotificationDto> markAllRead(@RequestHeader("X-User-Id") UUID userId) {
        notifications.markAllRead(userId);
        return notifications.list(userId);
    }

    @DeleteMapping("/{id}")
    void delete(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id) {
        notifications.delete(userId, id);
    }

    @DeleteMapping
    void deleteAll(@RequestHeader("X-User-Id") UUID userId) {
        notifications.deleteAll(userId);
    }

    @PostMapping("/internal/login")
    @ResponseStatus(HttpStatus.CREATED)
    List<NotificationDto> loginNotification(@RequestBody LoginNotificationRequest request) {
        return notifications.recordLogin(request);
    }
}
