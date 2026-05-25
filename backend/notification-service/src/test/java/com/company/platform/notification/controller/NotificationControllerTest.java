package com.company.platform.notification.controller;

import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.dto.NotificationDto;
import com.company.platform.commons.enums.NotificationCategory;
import com.company.platform.notification.dto.LoginNotificationRequest;
import com.company.platform.notification.service.NotificationService;
import org.testng.annotations.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class NotificationControllerTest {
    private final NotificationService service = mock(NotificationService.class);
    private final NotificationController controller = new NotificationController(service);

    @Test
    void delegatesUserNotificationEndpointsToService() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        NotificationDto dto = dto(id, false);
        given(service.list(userId)).willReturn(List.of(dto));
        given(service.markRead(userId, id)).willReturn(dto(id, true));

        assertEquals(controller.list(userId), List.of(dto));
        assertTrue(controller.markRead(userId, id).read());
        assertEquals(controller.markAllRead(userId), List.of(dto));
        controller.delete(userId, id);
        controller.deleteAll(userId);
        SseEmitter emitter = controller.stream(userId);

        verify(service).markAllRead(userId);
        verify(service).delete(userId, id);
        verify(service).deleteAll(userId);
        assertNotNull(emitter);
    }

    @Test
    void delegatesInternalSeedAndLoginEndpoints() {
        UUID userId = UUID.randomUUID();
        LoginNotificationRequest login = new LoginNotificationRequest(userId, "u@example.com", "User",
                "session-1", "127.0.0.1", "Chrome", "Asia/Kolkata", "2026-05-24 10:00", OffsetDateTime.now());
        DemoUserRequestDto demo = new DemoUserRequestDto(userId, "u@example.com", "User", "user", null);
        given(service.recordLogin(login)).willReturn(List.of(dto(UUID.randomUUID(), false)));
        given(service.seedDemoData(demo)).willReturn(List.of(dto(UUID.randomUUID(), false)));

        assertEquals(controller.loginNotification(login).size(), 1);
        assertEquals(controller.seedDemoData(demo).size(), 1);
    }

    private NotificationDto dto(UUID id, boolean read) {
        return new NotificationDto(id, NotificationCategory.AUTH, "Title", "Message", "/app", read, LocalDateTime.now());
    }
}
