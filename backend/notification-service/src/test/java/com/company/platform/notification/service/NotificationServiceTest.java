package com.company.platform.notification.service;

import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.enums.NotificationCategory;
import com.company.platform.commons.exception.ApiExceptions;
import com.company.platform.notification.dto.LoginNotificationRequest;
import com.company.platform.notification.model.Notification;
import com.company.platform.notification.repository.NotificationRepository;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class NotificationServiceTest {
    @Mock
    private NotificationRepository repository;

    private NotificationService service;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NotificationService(repository);
        ReflectionTestUtils.setField(service, "defaultTimeZone", "Asia/Kolkata");
        given(repository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID());
            }
            if (notification.getCreatedAt() == null) {
                notification.setCreatedAt(LocalDateTime.now());
            }
            return notification;
        });
    }

    @Test
    void listMapsNotificationsToDto() {
        UUID userId = UUID.randomUUID();
        Notification notification = notification(userId, NotificationCategory.AUTH, "Login", false);
        given(repository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of(notification));

        var rows = service.list(userId);
        assertEquals(rows.size(), 1);
        assertEquals(rows.getFirst().id(), notification.getId());
        assertEquals(rows.getFirst().title(), "Login");
        assertFalse(rows.getFirst().read());
    }

    @Test
    void markReadUpdatesOnlyOwnedNotification() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Notification notification = notification(userId, NotificationCategory.AUTH, "Login", false);
        given(repository.findByIdAndUserId(id, userId)).willReturn(Optional.of(notification));

        assertTrue(service.markRead(userId, id).read());
        assertTrue(notification.isRead());
    }

    @Test
    void markReadAndDeleteThrowWhenNotificationIsMissing() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        given(repository.findByIdAndUserId(id, userId)).willReturn(Optional.empty());
        given(repository.deleteByIdAndUserId(id, userId)).willReturn(0);

        expectThrows(ApiExceptions.ResourceNotFoundException.class, () -> service.markRead(userId, id));
        expectThrows(ApiExceptions.ResourceNotFoundException.class, () -> service.delete(userId, id));
    }

    @Test
    void bulkActionsDelegateToRepository() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        given(repository.deleteByIdAndUserId(id, userId)).willReturn(1);

        service.markAllRead(userId);
        service.delete(userId, id);
        service.deleteAll(userId);

        verify(repository).markAllReadByUserId(userId);
        verify(repository).deleteByIdAndUserId(id, userId);
        verify(repository).deleteByUserId(userId);
    }

    @Test
    void recordLoginCreatesLoginNotificationAndIdempotentAccountHints() {
        UUID userId = UUID.randomUUID();
        LoginNotificationRequest request = new LoginNotificationRequest(userId, "u@example.com", "User",
                "session-1", "203.0.113.10", "Chrome", "Asia/Kolkata", "2026-05-24 18:30", OffsetDateTime.now());
        given(repository.existsByUserIdAndTitle(userId, "Review your active sessions")).willReturn(false);
        given(repository.existsByUserIdAndTitle(userId, "Keep your profile up to date")).willReturn(true);

        assertEquals(service.recordLogin(request).size(), 1);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(captor.getAllValues().stream().map(Notification::getTitle).toList(),
                List.of("New login detected", "Review your active sessions"));
        assertTrue(String.valueOf(captor.getAllValues().getFirst().getMessage()).contains(String.valueOf("203.0.113.10")));
        assertTrue(String.valueOf(captor.getAllValues().getFirst().getMessage()).contains(String.valueOf("2026-05-24 18:30 (Asia/Kolkata)")));
        assertTrue(String.valueOf(captor.getAllValues().getFirst().getMessage()).contains(String.valueOf("Chrome")));
    }

    @Test
    void recordLoginFallsBackForBlankDeviceAndInvalidTimeZone() {
        UUID userId = UUID.randomUUID();
        LoginNotificationRequest request = new LoginNotificationRequest(userId, "u@example.com", "User",
                "session-1", "", "", "Invalid/Zone", "", OffsetDateTime.of(2026, 5, 24, 12, 0, 0, 0, ZoneOffset.UTC));

        service.recordLogin(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertTrue(String.valueOf(captor.getAllValues().getFirst().getMessage()).contains(String.valueOf("an unknown IP")));
        assertTrue(String.valueOf(captor.getAllValues().getFirst().getMessage()).contains(String.valueOf("an unknown device")));
        assertTrue(String.valueOf(captor.getAllValues().getFirst().getMessage()).contains(String.valueOf("IST")));
    }

    @Test
    void seedDemoDataCreatesOnlyMissingSamplesThenReturnsList() {
        UUID userId = UUID.randomUUID();
        DemoUserRequestDto request = new DemoUserRequestDto(userId, "u@example.com", "User", "user", null);
        given(repository.existsByUserIdAndTitle(userId, "Welcome to the platform")).willReturn(true);
        given(repository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of(notification(userId, NotificationCategory.SYSTEM, "Existing", false)));

        assertEquals(service.seedDemoData(request).size(), 1);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.argThat(n -> "Welcome to the platform".equals(n.getTitle())));
        verify(repository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    private Notification notification(UUID userId, NotificationCategory category, String title, boolean read) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setCategory(category);
        notification.setTitle(title);
        notification.setMessage("Message");
        notification.setActionUrl("/app");
        notification.setRead(read);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
