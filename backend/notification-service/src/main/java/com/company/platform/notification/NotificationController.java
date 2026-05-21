package com.company.platform.notification;

import com.company.platform.commons.dto.NotificationDto;
import com.company.platform.commons.enums.NotificationCategory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {
    @GetMapping
    List<NotificationDto> list() {
        return List.of(new NotificationDto(UUID.randomUUID(), NotificationCategory.SYSTEM, "Welcome", "Your notification stream is ready.", "/dashboard", false, LocalDateTime.now()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream() throws Exception {
        SseEmitter emitter = new SseEmitter(Duration.ofHours(6).toMillis());
        emitter.send(SseEmitter.event().name("notification").data(list().getFirst()));
        return emitter;
    }

    @PatchMapping("/{id}/read")
    void markRead(@PathVariable UUID id) {
    }

    @PatchMapping("/read-all")
    void markAllRead() {
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable UUID id) {
    }
}
